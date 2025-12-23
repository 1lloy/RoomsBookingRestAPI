package com.illoy.roombooking.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.illoy.roombooking.database.entity.BookingStatus;
import com.illoy.roombooking.dto.request.BookingCreateRequest;
import com.illoy.roombooking.dto.request.LoginRequest;
import com.illoy.roombooking.dto.request.RegisterRequest;
import com.illoy.roombooking.dto.request.RoomCreateEditRequest;
import com.illoy.roombooking.dto.response.BookingResponse;
import com.illoy.roombooking.dto.response.RoomResponse;
import com.illoy.roombooking.dto.response.UserResponse;
import com.illoy.roombooking.exception.ErrorResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@RequiredArgsConstructor
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Sql(scripts = "classpath:sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:sql/test_data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class BookingE2ETest {

    @Container
    private static final PostgreSQLContainer container = new PostgreSQLContainer("postgres:17.6");

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
    }

    @AfterAll
    static void tearDown() {
        if (container.isRunning()) {
            container.stop();
        }
    }

    // вспомогательный метод для получения JWT токена
    private String getAuthToken(String username, String password) {
        LoginRequest loginRequest =
                LoginRequest.builder().username(username).password(password).build();

        ResponseEntity<Map<String, Object>> loginResponse = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // предполагаем, что токен возвращается в поле "token" или "accessToken"
        Map<String, Object> responseBody = loginResponse.getBody();
        assertThat(responseBody).isNotNull();

        // попробуем разные возможные названия полей
        String token = (String) responseBody.get("token");
        if (token == null) {
            token = (String) responseBody.get("accessToken");
        }
        if (token == null) {
            token = (String) responseBody.get("jwt");
        }

        assertThat(token).isNotNull().isNotEmpty();
        return token;
    }

    // вспомогательный метод для создания заголовка с JWT
    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void completeBookingFlow_UserBooksRoom_Success() {
        // === 1. ADMIN логинится и получает токен ===
        String adminToken = getAuthToken("admin", "admin123");

        // === 2. ADMIN создает комнату ===
        RoomCreateEditRequest roomRequest = RoomCreateEditRequest.builder()
                .name("E2E Conference")
                .description("Room for E2E tests")
                .capacity(15)
                .build();
        HttpEntity<RoomCreateEditRequest> roomRequestEntity =
                new HttpEntity<>(roomRequest, createAuthHeaders(adminToken));

        ResponseEntity<RoomResponse> roomResponse =
                restTemplate.exchange("/api/admin/rooms", HttpMethod.POST, roomRequestEntity, RoomResponse.class);

        assertThat(roomResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(roomResponse.getBody()).isNotNull();
        Long roomId = roomResponse.getBody().getId();

        // === 3. USER регистрируется ===
        RegisterRequest userRequest = RegisterRequest.builder()
                .username("e2euser")
                .password("user123")
                .email("user@test.com")
                .build();
        ResponseEntity<UserResponse> userResponse =
                restTemplate.postForEntity("/api/auth/register", userRequest, UserResponse.class);

        assertThat(userResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // === 4. USER логинится и получает токен ===
        String userToken = getAuthToken("e2euser", "user123");

        // === 5. USER бронирует комнату ===
        BookingCreateRequest bookingRequest = BookingCreateRequest.builder()
                .roomId(roomId)
                .startTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0))
                .endTime(LocalDateTime.now().plusDays(1).withHour(12).withMinute(0))
                .build();

        HttpEntity<BookingCreateRequest> bookingRequestEntity =
                new HttpEntity<>(bookingRequest, createAuthHeaders(userToken));

        ResponseEntity<BookingResponse> bookingResponse =
                restTemplate.exchange("/api/bookings", HttpMethod.POST, bookingRequestEntity, BookingResponse.class);

        assertThat(bookingResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bookingResponse.getBody()).isNotNull();
        Long bookingId = bookingResponse.getBody().getId();

        // === 6. Проверяем, что бронь видна в списке ===
        HttpEntity<Void> getBookingsEntity = new HttpEntity<>(createAuthHeaders(userToken));

        ResponseEntity<List<BookingResponse>> bookingsResponse = restTemplate
                .withBasicAuth("e2euser", "user123")
                .exchange(
                        "/api/users/me/bookings",
                        HttpMethod.GET,
                        getBookingsEntity,
                        new ParameterizedTypeReference<List<BookingResponse>>() {});

        assertThat(bookingsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bookingsResponse.getBody()).hasSize(1);
        assertThat(bookingsResponse.getBody().getFirst().getId()).isEqualTo(bookingId);

        // === 7. Пользователь проверяет свой профиль (возьмем id из UserResponse) ===
        ResponseEntity<UserResponse> userResponseAfterMe = restTemplate.exchange(
                "/api/users/me",
                HttpMethod.GET,
                getBookingsEntity, // используем тот же заголовок
                UserResponse.class);

        Assertions.assertNotNull(userResponseAfterMe.getBody());
        Long userId = userResponseAfterMe.getBody().getId();

        // === 8. ADMIN видит бронь в системе ===
        HttpEntity<Void> adminRequestEntity = new HttpEntity<>(createAuthHeaders(adminToken));

        ResponseEntity<Map<String, Object>> userBookingsResponse = restTemplate.exchange(
                "/api/admin/bookings/user/{userId}?page=0&size=20",
                HttpMethod.GET,
                adminRequestEntity,
                new ParameterizedTypeReference<Map<String, Object>>() {},
                userId);

        assertThat(userBookingsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertNotNull(userBookingsResponse.getBody());

        assertThat(userBookingsResponse.getBody())
                .isNotNull()
                .extracting("content")
                .asInstanceOf(InstanceOfAssertFactories.LIST) // типобезопасно
                .hasSize(1)
                .first()
                .asInstanceOf(InstanceOfAssertFactories.MAP) // типобезопасно
                .containsEntry("userName", "e2euser")
                .containsEntry("roomName", "E2E Conference");
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void bookingConflict_TwoUsersSameTime_SecondUserGetsError() {

        // === 1. ADMIN логинится и получает токен ===
        String adminToken = getAuthToken("admin", "admin123");

        // === 2. ADMIN создает комнату ===
        RoomCreateEditRequest roomRequest = RoomCreateEditRequest.builder()
                .name("Conflict Room")
                .description("Room for E2E tests")
                .capacity(15)
                .build();
        HttpEntity<RoomCreateEditRequest> roomRequestEntity =
                new HttpEntity<>(roomRequest, createAuthHeaders(adminToken));

        ResponseEntity<RoomResponse> roomResponse =
                restTemplate.exchange("/api/admin/rooms", HttpMethod.POST, roomRequestEntity, RoomResponse.class);

        assertThat(roomResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(roomResponse.getBody()).isNotNull();
        Long roomId = roomResponse.getBody().getId();

        // When - первый пользователь успешно бронирует
        String user1 = "user1";
        String user2 = "user2";

        // === USER_1 регистрируется ===
        RegisterRequest userRequest1 = RegisterRequest.builder()
                .username(user1)
                .password("user1pass")
                .email("user1@test.com")
                .build();
        ResponseEntity<ErrorResponse> userResponse1 =
                restTemplate.postForEntity("/api/auth/register", userRequest1, ErrorResponse.class);

        Assertions.assertNotNull(userResponse1.getBody());
        System.out.println(userResponse1.getBody().getError());
        assertThat(userResponse1.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // === USER_1 логинится и получает токен ===
        String user1Token = getAuthToken(user1, "user1pass");

        // === USER_2 регистрируется ===
        RegisterRequest userRequest2 = RegisterRequest.builder()
                .username(user2)
                .password("user2pass")
                .email("user2@test.com")
                .build();
        ResponseEntity<UserResponse> userResponse2 =
                restTemplate.postForEntity("/api/auth/register", userRequest2, UserResponse.class);

        assertThat(userResponse2.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // === USER_2 логинится и получает токен ===
        String user2Token = getAuthToken(user2, "user2pass");

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(14);
        LocalDateTime end = LocalDateTime.now().plusDays(1).withHour(16);

        // user_1 бронирует комнату
        BookingCreateRequest request1 = BookingCreateRequest.builder()
                .roomId(roomId)
                .startTime(start)
                .endTime(end)
                .build();

        HttpEntity<BookingCreateRequest> bookingRequestEntity1 =
                new HttpEntity<>(request1, createAuthHeaders(user1Token));

        ResponseEntity<BookingResponse> response1 =
                restTemplate.exchange("/api/bookings", HttpMethod.POST, bookingRequestEntity1, BookingResponse.class);

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Assertions.assertNotNull(response1.getBody());
        assertThat(response1.getBody().getRoomId()).isEqualTo(roomId);
        assertThat(response1.getBody().getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        // user_2 бронирует ту же комнату и получает ошибку bad_request
        BookingCreateRequest request2 = BookingCreateRequest.builder()
                .roomId(roomId)
                .startTime(start)
                .endTime(end)
                .build();

        HttpEntity<BookingCreateRequest> bookingRequestEntity2 =
                new HttpEntity<>(request2, createAuthHeaders(user2Token));

        ResponseEntity<ErrorResponse> response2 =
                restTemplate.exchange("/api/bookings", HttpMethod.POST, bookingRequestEntity2, ErrorResponse.class);

        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertNotNull(response2.getBody());
        assertThat(response2.getBody().getError()).isEqualTo("ROOM_NOT_AVAILABLE_FOR_THIS_TIME");
    }
}

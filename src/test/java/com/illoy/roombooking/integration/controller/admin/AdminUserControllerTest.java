package com.illoy.roombooking.integration.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.illoy.roombooking.database.entity.*;
import com.illoy.roombooking.database.repository.BookingRepository;
import com.illoy.roombooking.database.repository.RoomRepository;
import com.illoy.roombooking.database.repository.UserRepository;
import com.illoy.roombooking.dto.request.LoginRequest;
import com.illoy.roombooking.dto.request.UserStatusUpdateRequest;
import com.illoy.roombooking.dto.response.JwtResponse;
import com.illoy.roombooking.dto.response.UserResponse;
import com.illoy.roombooking.exception.ErrorResponse;
import com.illoy.roombooking.integration.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public class AdminUserControllerTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;

    private Long USER_ID;

    @BeforeEach
    void setup() throws Exception {
        User user2 = User.builder()
                .username("anna")
                .email("anna@gmail.com")
                .password(passwordEncoder.encode("123"))
                .role(UserRole.ROLE_USER)
                .isActive(true)
                .build();
        User user3 = User.builder()
                .username("oleg")
                .email("oleg@gmail.com")
                .password(passwordEncoder.encode("123"))
                .role(UserRole.ROLE_USER)
                .isActive(true)
                .build();
        User user4 = User.builder()
                .username("nikol")
                .email("kolya@gmail.com")
                .password(passwordEncoder.encode("123"))
                .role(UserRole.ROLE_USER)
                .isActive(false)
                .build();
        User admin = User.builder()
                .username("admin1")
                .email("mark@gmail.com")
                .password(passwordEncoder.encode("123"))
                .role(UserRole.ROLE_ADMIN)
                .isActive(true)
                .build();

        userRepository.saveAll(List.of(user2, user3, user4, admin));
        USER_ID = user4.getId();

        Room room1 = Room.builder()
                .name("Conference Room A")
                .capacity(20)
                .isActive(true)
                .build();
        Room room2 = Room.builder()
                .name("Meeting Room B")
                .capacity(10)
                .isActive(true)
                .build();
        Room room3 =
                Room.builder().name("Small Room C").capacity(2).isActive(true).build();
        Room room4 = Room.builder()
                .name("Training Room D")
                .capacity(15)
                .isActive(false)
                .build();

        roomRepository.saveAll(List.of(room1, room2, room3, room4));

        Booking booking1 = Booking.builder()
                .room(room1)
                .user(user2)
                .startTime(LocalDateTime.of(2025, 1, 20, 9, 0))
                .endTime(LocalDateTime.of(2025, 1, 20, 10, 30))
                .status(BookingStatus.COMPLETED)
                .build();

        Booking booking2 = Booking.builder()
                .room(room2)
                .user(user3)
                .startTime(LocalDateTime.of(2026, 1, 20, 10, 0))
                .endTime(LocalDateTime.of(2026, 1, 20, 11, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking booking3 = Booking.builder()
                .room(room1)
                .user(user3)
                .startTime(LocalDateTime.of(2026, 1, 20, 11, 0))
                .endTime(LocalDateTime.of(2026, 1, 20, 12, 30))
                .status(BookingStatus.CANCELLED)
                .build();

        Booking booking4 = Booking.builder()
                .room(room3)
                .user(user2)
                .startTime(LocalDateTime.of(2026, 1, 20, 14, 0))
                .endTime(LocalDateTime.of(2026, 1, 20, 15, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking booking5 = Booking.builder()
                .room(room3)
                .user(user2)
                .startTime(LocalDateTime.of(2026, 1, 22, 15, 30))
                .endTime(LocalDateTime.of(2026, 1, 22, 17, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        bookingRepository.saveAll(List.of(booking1, booking2, booking3, booking4, booking5));

        LoginRequest loginRequest =
                LoginRequest.builder().username("admin1").password("123").build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JwtResponse jwtResponse = objectMapper.readValue(result.getResponse().getContentAsString(), JwtResponse.class);
        jwtToken = jwtResponse.getToken();
    }

    @Test
    void findAll_shouldReturnUserResponse() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/users/all")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        List<UserResponse> users =
                objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

        assertThat(users).hasSize(4);
        assertThat(users).anyMatch(user -> user.getRole() == UserRole.ROLE_ADMIN);
        assertThat(users).anyMatch(user -> !user.isActive());
    }

    @Test
    void findAllActive_shouldReturnPage() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/users/all/active")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "username")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Преобразуйте JSON в строку и используйте JsonPath
        String jsonResponse = result.getResponse().getContentAsString();

        // Детальные проверки с AssertJ
        List<String> userNames = JsonPath.read(jsonResponse, "$.content[*].username");
        List<Boolean> statuses = JsonPath.read(jsonResponse, "$.content[*].active");

        assertThat(userNames).hasSize(2);
        assertThat(statuses).containsOnly(true);
        assertThat(userNames).containsExactly("anna", "oleg");
    }

    @Test
    void findByRole_shouldReturnPage() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/users/by-role")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("role", "ROLE_USER")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "email")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Преобразуйте JSON в строку и используйте JsonPath
        String jsonResponse = result.getResponse().getContentAsString();

        // Детальные проверки с AssertJ
        List<String> userEmails = JsonPath.read(jsonResponse, "$.content[*].email");
        List<String> userRoles = JsonPath.read(jsonResponse, "$.content[*].role");
        List<Boolean> statuses = JsonPath.read(jsonResponse, "$.content[*].active");

        assertThat(userEmails).hasSize(2);
        assertThat(statuses).contains(false);
        assertThat(userRoles).allMatch(userRole -> !userRole.equals(UserRole.ROLE_ADMIN.name()));
        assertThat(userEmails).containsExactly("anna@gmail.com", "kolya@gmail.com");
    }

    @Test
    void findByEmail_shouldReturnUserResponseSuccess() throws Exception {
        String email = "kolya@gmail.com";
        MvcResult result = mockMvc.perform(get("/api/admin/users/by-email")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(email))
                .andExpect(status().isOk())
                .andReturn();

        UserResponse userResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), UserResponse.class);

        assertThat(userResponse).isNotNull();
        assertThat(userResponse.getRole()).isEqualTo(UserRole.ROLE_USER);
        assertThat(userResponse.getEmail()).isEqualTo("kolya@gmail.com");
    }

    @Test
    void findByEmail_shouldReturnUsernameNotFoundException() throws Exception {
        String email = "notExistingEmail@gmail.com";
        MvcResult result = mockMvc.perform(get("/api/admin/users/by-email")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(email))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse errorResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        // then
        assertEquals("USER_NOT_FOUND", errorResponse.getError());
        assertEquals("User not found with email: notExistingEmail@gmail.com", errorResponse.getMessage());
        assertEquals(404, errorResponse.getStatus());
        assertThat(errorResponse.getCertainErrors()).isNullOrEmpty();
    }

    @Test
    void findByUsername_shouldReturnUserResponseSuccess() throws Exception {
        String username = "admin1";
        MvcResult result = mockMvc.perform(get("/api/admin/users/by-username")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(username))
                .andExpect(status().isOk())
                .andReturn();

        UserResponse userResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), UserResponse.class);

        assertThat(userResponse).isNotNull();
        assertThat(userResponse.getRole()).isEqualTo(UserRole.ROLE_ADMIN);
        assertThat(userResponse.getUsername()).isEqualTo("admin1");
    }

    @Test
    void findByUsername_shouldReturnUsernameNotFoundException() throws Exception {
        String username = "notExistingUsername";

        MvcResult result = mockMvc.perform(get("/api/admin/users/by-username")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(username))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse errorResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        // then
        assertEquals("USER_NOT_FOUND", errorResponse.getError());
        assertEquals("User not found with username: notExistingUsername", errorResponse.getMessage());
        assertEquals(404, errorResponse.getStatus());
        assertThat(errorResponse.getCertainErrors()).isNullOrEmpty();
    }

    @Test
    void updateUserStatus_shouldReturnMapSuccess() throws Exception {
        UserStatusUpdateRequest request =
                UserStatusUpdateRequest.builder().active(true).build();

        MvcResult result = mockMvc.perform(patch("/api/admin/users/{userId}/status", USER_ID)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Получаем мапу из ответа
        String content = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(content, new TypeReference<>() {});

        assertThat(responseMap.get("success")).isEqualTo(true);
        assertEquals(responseMap.get("userId"), USER_ID.intValue());
        assertTrue((Boolean) responseMap.get("active"));
    }

    @Test
    void updateUserStatus_shouldReturnUsernameStatusConflictException() throws Exception {
        UserStatusUpdateRequest request =
                UserStatusUpdateRequest.builder().active(false).build();

        MvcResult result = mockMvc.perform(patch("/api/admin/users/{userId}/status", USER_ID)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse errorResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        // then
        assertEquals("USER_STATUS_CONFLICT", errorResponse.getError());
        assertEquals("Username already has this status", errorResponse.getMessage());
        assertEquals(400, errorResponse.getStatus());
        assertThat(errorResponse.getCertainErrors()).isNullOrEmpty();
    }

    @Test
    void updateUserStatus_shouldReturnUsernameNotFoundException() throws Exception {
        UserStatusUpdateRequest request =
                UserStatusUpdateRequest.builder().active(false).build();

        MvcResult result = mockMvc.perform(patch("/api/admin/users/{userId}/status", -999L)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse errorResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        // then
        assertEquals("USER_NOT_FOUND", errorResponse.getError());
        assertEquals("Failed to retrieve user with id: -999", errorResponse.getMessage());
        assertEquals(404, errorResponse.getStatus());
        assertThat(errorResponse.getCertainErrors()).isNullOrEmpty();
    }
}

package com.illoy.roombooking.integration.controller.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.illoy.roombooking.database.entity.*;
import com.illoy.roombooking.database.repository.BookingRepository;
import com.illoy.roombooking.database.repository.RoomRepository;
import com.illoy.roombooking.database.repository.UserRepository;
import com.illoy.roombooking.dto.request.BookingStatusUpdateRequest;
import com.illoy.roombooking.dto.request.LoginRequest;
import com.illoy.roombooking.dto.response.BookingResponse;
import com.illoy.roombooking.dto.response.JwtResponse;
import com.illoy.roombooking.dto.response.UserResponse;
import com.illoy.roombooking.exception.ErrorResponse;
import com.illoy.roombooking.integration.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
@AutoConfigureMockMvc
public class AdminBookingControllerTest extends IntegrationTestBase {

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    private String jwtToken;

    private Long ROOM_ID;
    private Room ROOM;
    private User USER;
    private Long BOOKING_ID;

    @BeforeEach
    void setup() throws Exception {
        User user2 = User.builder().username("anna").email("anna@gmail.com").password(passwordEncoder.encode("123")).role(UserRole.ROLE_USER).isActive(true).build();
        User user3 = User.builder().username("oleg").email("oleg@gmail.com").password(passwordEncoder.encode("123")).role(UserRole.ROLE_USER).isActive(true).build();
        User user4 = User.builder().username("nikol").email("kolya@gmail.com").password(passwordEncoder.encode("123")).role(UserRole.ROLE_USER).isActive(false).build();
        User admin = User.builder().username("admin1").email("mark@gmail.com").password(passwordEncoder.encode("123")).role(UserRole.ROLE_ADMIN).isActive(true).build();

        userRepository.saveAll(List.of(user2, user3, user4, admin));

        USER = user2;

        Room room1 = Room.builder().name("Conference Room A").capacity(20).isActive(true).build();
        Room room2 = Room.builder().name("Meeting Room B").capacity(10).isActive(true).build();
        Room room3 = Room.builder().name("Small Room C").capacity(2).isActive(true).build();
        Room room4 = Room.builder().name("Training Room D").capacity(15).isActive(false).build();

        roomRepository.saveAll(List.of(room1, room2, room3, room4));

        ROOM_ID = room2.getId();
        ROOM = room2;

        Booking booking1 = Booking.builder().room(room1).user(user2)
                .startTime(LocalDateTime.of(2025, 1, 20, 9, 0))
                .endTime(LocalDateTime.of(2025, 1, 20, 10, 30))
                .status(BookingStatus.COMPLETED)
                .build();

        Booking booking2 = Booking.builder().room(room2).user(user3)
                .startTime(LocalDateTime.of(2100, 1, 20, 10, 0))
                .endTime(LocalDateTime.of(2100, 1, 20, 11, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking booking3 = Booking.builder().room(room1).user(user3)
                .startTime(LocalDateTime.of(2100, 1, 20, 11, 0))
                .endTime(LocalDateTime.of(2100, 1, 20, 12, 30))
                .status(BookingStatus.CANCELLED)
                .build();

        Booking booking4 = Booking.builder().room(room3).user(user2)
                .startTime(LocalDateTime.of(2100, 1, 20, 14, 0))
                .endTime(LocalDateTime.of(2100, 1, 20, 15, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking booking5 = Booking.builder().room(room3).user(user2)
                .startTime(LocalDateTime.of(2100, 1, 22, 15, 30))
                .endTime(LocalDateTime.of(2100, 1, 22, 17, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        bookingRepository.saveAll(List.of(booking1, booking2, booking3, booking4, booking5));

        BOOKING_ID = booking4.getId();

        LoginRequest loginRequest = LoginRequest.builder().username("admin1").password("123").build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JwtResponse jwtResponse = objectMapper.readValue(result.getResponse().getContentAsString(), JwtResponse.class);
        jwtToken = jwtResponse.getToken();
    }

    @Test
    void findAllByStatus_shouldReturnPageWithOneStatus() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("status", "CONFIRMED")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "startTime")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Преобразуйте JSON в строку и используйте JsonPath
        String jsonResponse = result.getResponse().getContentAsString();

        // Детальные проверки с AssertJ
        List<String> statuses = JsonPath.read(jsonResponse, "$.content[*].status");
        List<String> usernames = JsonPath.read(jsonResponse, "$.content[*].userName");
        List<String> startTimes = JsonPath.read(jsonResponse, "$.content[*].startTime");

        assertThat(statuses).hasSize(3);
        assertThat(statuses).containsOnly("CONFIRMED");
        assertThat(usernames).containsExactlyInAnyOrder("anna", "anna", "oleg");
        assertThat(LocalDateTime.parse(startTimes.getFirst())).isEqualTo(LocalDateTime.of(2100, 1, 22, 15, 30, 0));
    }

    @Test
    void findAllByStatus_shouldReturnPageWithNoStatus() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "startTime")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        List<BookingResponse> bookings = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>(){});

        assertThat(bookings).hasSize(5);
        assertThat(bookings).anyMatch(booking -> booking.getStatus() != BookingStatus.CANCELLED);
    }

    @Test
    void findUserBookings_shouldReturnPageSuccess() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/bookings/user/{userId}", USER.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "startTime")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Преобразуйте JSON в строку и используйте JsonPath
        String jsonResponse = result.getResponse().getContentAsString();

        // Детальные проверки с AssertJ
        List<String> statuses = JsonPath.read(jsonResponse, "$.content[*].status");
        List<Integer> userIds = JsonPath.read(jsonResponse, "$.content[*].userId");

        assertThat(statuses).hasSize(3);
        assertThat(statuses).anyMatch(status -> status.equals("COMPLETED"));
        assertThat(userIds).containsOnly(USER.getId().intValue());
    }

    @Test
    void findUserBookings_shouldReturnUserNotFoundException() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/bookings/user/{userId}", -999L)
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "startTime")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertEquals("USER_NOT_FOUND", errorResponse.getError());
        assertEquals("User not found with id: -999", errorResponse.getMessage());
        assertEquals(404, errorResponse.getStatus());
        assertThat(errorResponse.getCertainErrors()).isNullOrEmpty();
    }

    @Test
    void updateBookingStatus_shouldReturnBookingResponse() throws Exception {
        BookingStatusUpdateRequest request = BookingStatusUpdateRequest.builder().status(BookingStatus.CANCELLED).build();

        MvcResult result = mockMvc.perform(patch("/api/admin/bookings/{bookingId}/status", BOOKING_ID)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        BookingResponse bookingResponse = objectMapper.readValue(result.getResponse().getContentAsString(), BookingResponse.class);

        assertEquals(BookingStatus.CANCELLED, bookingResponse.getStatus());
        assertEquals(BOOKING_ID, bookingResponse.getId());
    }

    @Test
    void updateBookingStatus_shouldReturnBookingNotFoundException() throws Exception {
        BookingStatusUpdateRequest request = BookingStatusUpdateRequest.builder().status(BookingStatus.CANCELLED).build();

        MvcResult result = mockMvc.perform(patch("/api/admin/bookings/{bookingId}/status", -999L)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertEquals("BOOKING_NOT_FOUND", errorResponse.getError());
        assertEquals("Booking not found with id: -999", errorResponse.getMessage());
        assertEquals(404, errorResponse.getStatus());
        assertThat(errorResponse.getCertainErrors()).isNullOrEmpty();
    }

    @Test
    void updateBookingStatus_shouldReturnBookingStatusConflictException() throws Exception {
        BookingStatusUpdateRequest request = BookingStatusUpdateRequest.builder().status(BookingStatus.CONFIRMED).build();

        MvcResult result = mockMvc.perform(patch("/api/admin/bookings/{bookingId}/status", BOOKING_ID)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertEquals("BOOKING_STATUS_CONFLICT", errorResponse.getError());
        assertEquals("Booking already has status: CONFIRMED", errorResponse.getMessage());
        assertEquals(400, errorResponse.getStatus());
        assertThat(errorResponse.getCertainErrors()).isNullOrEmpty();
    }
}

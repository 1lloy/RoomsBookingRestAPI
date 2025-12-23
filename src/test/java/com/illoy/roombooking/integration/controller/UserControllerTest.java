package com.illoy.roombooking.integration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.illoy.roombooking.database.entity.*;
import com.illoy.roombooking.database.repository.BookingRepository;
import com.illoy.roombooking.database.repository.RoomRepository;
import com.illoy.roombooking.database.repository.UserRepository;
import com.illoy.roombooking.dto.request.LoginRequest;
import com.illoy.roombooking.dto.request.UserEditRequest;
import com.illoy.roombooking.dto.response.BookingResponse;
import com.illoy.roombooking.dto.response.JwtResponse;
import com.illoy.roombooking.dto.response.UserResponse;
import com.illoy.roombooking.exception.ErrorResponse;
import com.illoy.roombooking.integration.IntegrationTestBase;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public class UserControllerTest extends IntegrationTestBase {

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

    private Long userId;
    private String jwtToken;

    @BeforeEach
    void setup() throws Exception {
        User user = User.builder()
                .username("john")
                .email("johnHjon@gmail.com")
                .password(passwordEncoder.encode("123"))
                .role(UserRole.ROLE_USER)
                .isActive(true)
                .build();

        userRepository.save(user);

        userId = user.getId();

        Room room = Room.builder().name("A Room").capacity(10).isActive(true).build();

        roomRepository.save(room);

        Booking booking1 = Booking.builder()
                .room(room)
                .user(user)
                .startTime(LocalDateTime.of(2026, 2, 10, 10, 0))
                .endTime(LocalDateTime.of(2026, 2, 10, 11, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking booking2 = Booking.builder()
                .room(room)
                .user(user)
                .startTime(LocalDateTime.of(2026, 2, 12, 12, 0))
                .endTime(LocalDateTime.of(2026, 2, 12, 13, 0))
                .status(BookingStatus.CANCELLED)
                .build();

        bookingRepository.saveAll(List.of(booking1, booking2));

        LoginRequest loginRequest =
                LoginRequest.builder().username("john").password("123").build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JwtResponse jwtResponse = objectMapper.readValue(result.getResponse().getContentAsString(), JwtResponse.class);
        jwtToken = jwtResponse.getToken();
    }

    @Test
    void getCurrentUser_shouldReturnUserResponseSuccess() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        UserResponse userResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), UserResponse.class);

        assertEquals("john", userResponse.getUsername());
        assertEquals("johnHjon@gmail.com", userResponse.getEmail());
        assertEquals(UserRole.ROLE_USER, userResponse.getRole());
        assertThat(userResponse.isActive()).isTrue();
    }

    @Test
    void getCurrentUser_shouldReturnUnauthorized_whenJwtIsMissing() throws Exception {
        mockMvc.perform(get("/api/users/me").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUser_shouldReturnUnauthorized_whenJwtIsInvalid() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer invalid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateCurrentUser_shouldReturnUpdatedUserResponse() throws Exception {
        // given
        UserEditRequest editRequest = UserEditRequest.builder()
                .email("john_updated@gmail.com")
                .password("newPassword123")
                .build();

        // when
        MvcResult result = mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editRequest)))
                .andExpect(status().isOk())
                .andReturn();

        UserResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), UserResponse.class);

        User updatedUser = userRepository.findByEmail("john_updated@gmail.com").orElseThrow();

        // then
        assertEquals("john", response.getUsername());
        assertEquals("john_updated@gmail.com", response.getEmail());
        assertThat(passwordEncoder.matches("newPassword123", updatedUser.getPassword()))
                .isTrue();
    }

    @Test
    void updateCurrentUser_shouldReturnUnauthorized_whenJwtIsMissing() throws Exception {
        UserEditRequest editRequest = UserEditRequest.builder()
                .email("john_updated@gmail.com")
                .password("newPassword123")
                .build();

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateCurrentUser_shouldReturnBadRequest_whenValidationFails() throws Exception {
        // given
        UserEditRequest editRequest =
                UserEditRequest.builder().email("joEmail").password("pass").build();

        // when
        MvcResult result = mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editRequest)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse errorResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        // then
        assertEquals("VALIDATION_FAILED", errorResponse.getError());
        assertEquals("Request validation failed", errorResponse.getMessage());
        assertEquals(400, errorResponse.getStatus());
        assertEquals(
                "Пароль должен иметь длину от 6 до 40 символов",
                errorResponse.getCertainErrors().get("password"));
        assertEquals(
                "Адрес электронный почты не является корректным",
                errorResponse.getCertainErrors().get("email"));
    }

    @Test
    void getUserBookings_shouldReturnFilteredBookingsSuccess() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/users/me/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "CONFIRMED")
                        .param("fromDate", "2026-02-10")
                        .param("toDate", "2026-02-20"))
                .andExpect(status().isOk())
                .andReturn();

        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        List<BookingResponse> bookings =
                objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

        assertThat(bookings).isNotEmpty();
        assertThat(bookings).hasSize(1);
        assertThat(bookings)
                .allMatch(b -> b.getUserId().equals(userId))
                .allMatch(b -> b.getStatus() == BookingStatus.CONFIRMED);

        assertThat(bookings)
                .allMatch(b ->
                        !b.getStartTime().isBefore(LocalDate.of(2026, 2, 10).atStartOfDay()))
                .allMatch(
                        b -> !b.getStartTime().isAfter(LocalDate.of(2026, 2, 20).atTime(LocalTime.MAX)));
    }
}

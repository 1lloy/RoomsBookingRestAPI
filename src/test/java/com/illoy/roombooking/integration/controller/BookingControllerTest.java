package com.illoy.roombooking.integration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.illoy.roombooking.database.entity.*;
import com.illoy.roombooking.database.repository.BookingRepository;
import com.illoy.roombooking.database.repository.RoomRepository;
import com.illoy.roombooking.database.repository.UserRepository;
import com.illoy.roombooking.dto.request.BookingCreateRequest;
import com.illoy.roombooking.dto.request.LoginRequest;
import com.illoy.roombooking.dto.response.BookingResponse;
import com.illoy.roombooking.dto.response.JwtResponse;
import com.illoy.roombooking.exception.ErrorResponse;
import com.illoy.roombooking.integration.IntegrationTestBase;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public class BookingControllerTest extends IntegrationTestBase {

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
    private Long ACTIVE_ROOM_ID;
    private Long INACTIVE_ROOM_ID;
    private Long CONFLICT_ROOM_ID;
    private Room CANCELLING_ROOM;
    private User CANCELLING_USER;
    private Long CANCELLING_BOOKING_ID;
    private Long NORMAL_BOOKING_ID;

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
        User admin = User.builder()
                .username("admin1")
                .email("mark@gmail.com")
                .password(passwordEncoder.encode("123"))
                .role(UserRole.ROLE_ADMIN)
                .isActive(true)
                .build();

        userRepository.saveAll(List.of(user2, user3, admin));

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

        ACTIVE_ROOM_ID = room1.getId();
        CONFLICT_ROOM_ID = room2.getId();
        INACTIVE_ROOM_ID = room4.getId();
        CANCELLING_ROOM = room3;
        CANCELLING_USER = user2;

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

        CANCELLING_BOOKING_ID = booking2.getId();

        NORMAL_BOOKING_ID = booking4.getId();

        LoginRequest loginRequest =
                LoginRequest.builder().username("anna").password("123").build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JwtResponse jwtResponse = objectMapper.readValue(result.getResponse().getContentAsString(), JwtResponse.class);
        jwtToken = jwtResponse.getToken();
    }

    @Test
    void create_shouldCreateBookingAndReturnCreatedStatus() throws Exception {
        BookingCreateRequest request = BookingCreateRequest.builder()
                .roomId(ACTIVE_ROOM_ID)
                .startTime(LocalDateTime.of(2026, 2, 15, 10, 0))
                .endTime(LocalDateTime.of(2026, 2, 15, 11, 0))
                .build();

        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        BookingResponse bookingResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), BookingResponse.class);

        assertThat(bookingResponse).isNotNull();
        assertThat(bookingResponse.getId()).isNotNull();
        assertThat(bookingResponse.getRoomId()).isEqualTo(ACTIVE_ROOM_ID);
        assertThat(bookingResponse.getStartTime()).isEqualTo(request.getStartTime());
        assertThat(bookingResponse.getEndTime()).isEqualTo(request.getEndTime());
        assertThat(bookingResponse.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void create_withInvalidRoomId_shouldReturnBadRequest() throws Exception {
        Long invalidRoomId = -999L;

        BookingCreateRequest request = BookingCreateRequest.builder()
                .roomId(invalidRoomId)
                .startTime(LocalDateTime.of(2026, 2, 15, 10, 0))
                .endTime(LocalDateTime.of(2026, 2, 15, 11, 0))
                .build();

        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse errorResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertEquals("ROOM_NOT_FOUND", errorResponse.getError());
        assertEquals("Room not found or inactive with id: " + invalidRoomId, errorResponse.getMessage());
        assertEquals(404, errorResponse.getStatus());
    }

    @Test
    void create_withInvalidRoomId_shouldReturnBadRequestInactive() throws Exception {
        BookingCreateRequest request = BookingCreateRequest.builder()
                .roomId(INACTIVE_ROOM_ID)
                .startTime(LocalDateTime.of(2026, 2, 15, 10, 0))
                .endTime(LocalDateTime.of(2026, 2, 15, 11, 0))
                .build();

        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse errorResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertEquals("ROOM_NOT_FOUND", errorResponse.getError());
        assertEquals("Room not found or inactive with id: " + INACTIVE_ROOM_ID, errorResponse.getMessage());
        assertEquals(404, errorResponse.getStatus());
    }

    @Test
    void create_withOverlappingBooking_shouldReturnConflict() throws Exception {
        BookingCreateRequest request = BookingCreateRequest.builder()
                .roomId(CONFLICT_ROOM_ID)
                .startTime(LocalDateTime.of(2026, 1, 20, 10, 30))
                .endTime(LocalDateTime.of(2026, 1, 20, 12, 30))
                .build();

        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse errorResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertEquals("ROOM_NOT_AVAILABLE_FOR_THIS_TIME", errorResponse.getError());
        assertEquals("Room is not available for selected time", errorResponse.getMessage());
        assertEquals(400, errorResponse.getStatus());
    }

    @Test
    void create_withInvalidData_shouldReturnBadRequest() throws Exception {
        BookingCreateRequest request = BookingCreateRequest.builder()
                .roomId(ACTIVE_ROOM_ID)
                .startTime(LocalDateTime.of(2026, 2, 15, 11, 0))
                .endTime(LocalDateTime.of(2026, 2, 15, 10, 0))
                .build();

        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse errorResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertEquals("BOOKING_TIME_CONFLICT", errorResponse.getError());
        assertEquals("End time must be after start time", errorResponse.getMessage());
        assertEquals(400, errorResponse.getStatus());
    }

    @Test
    void create_withoutAuthentication_shouldReturnUnauthorized() throws Exception {
        BookingCreateRequest request = BookingCreateRequest.builder()
                .roomId(ACTIVE_ROOM_ID)
                .startTime(LocalDateTime.of(2026, 2, 15, 10, 0))
                .endTime(LocalDateTime.of(2026, 2, 15, 11, 0))
                .build();

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancel_shouldCancelBookingAndReturnBookingResponseSuccess() throws Exception {
        Booking booking6 = Booking.builder()
                .room(CANCELLING_ROOM)
                .user(CANCELLING_USER)
                .startTime(LocalDateTime.of(2026, 1, 22, 17, 30))
                .endTime(LocalDateTime.of(2026, 1, 22, 18, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking savedBooking = bookingRepository.save(booking6);

        MvcResult result = mockMvc.perform(patch("/api/bookings/{bookingId}/cancel", savedBooking.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        BookingResponse bookingResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), BookingResponse.class);

        assertThat(bookingResponse).isNotNull();
        assertThat(bookingResponse.getId()).isEqualTo(savedBooking.getId());
        assertThat(bookingResponse.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancel_withNonExistingBooking_shouldReturnNotFound() throws Exception {
        Long nonExistingId = -999L;

        MvcResult result = mockMvc.perform(patch("/api/bookings/{bookingId}/cancel", nonExistingId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse errorResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertEquals("BOOKING_NOT_FOUND", errorResponse.getError());
        assertEquals("Booking not found", errorResponse.getMessage());
        assertEquals(404, errorResponse.getStatus());
    }

    @Test
    void cancel_withNonExistingBooking_shouldReturnForbidden() throws Exception {
        MvcResult result = mockMvc.perform(patch("/api/bookings/{bookingId}/cancel", CANCELLING_BOOKING_ID)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andReturn();

        ErrorResponse errorResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertEquals("ACCESS_DENIED", errorResponse.getError());
        assertEquals("You can only cancel your own bookings", errorResponse.getMessage());
        assertEquals(403, errorResponse.getStatus());
    }

    @Test
    void cancel_withNonExistingBooking_shouldReturnBookingStatusConflict() throws Exception {
        Booking booking6 = Booking.builder()
                .room(CANCELLING_ROOM)
                .user(CANCELLING_USER)
                .startTime(LocalDateTime.of(2026, 1, 22, 17, 30))
                .endTime(LocalDateTime.of(2026, 1, 22, 18, 0))
                .status(BookingStatus.CANCELLED)
                .build();

        Booking savedBooking = bookingRepository.save(booking6);

        MvcResult result = mockMvc.perform(patch("/api/bookings/{bookingId}/cancel", savedBooking.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse errorResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertEquals("BOOKING_STATUS_CONFLICT", errorResponse.getError());
        assertEquals("Booking already cancelled", errorResponse.getMessage());
        assertEquals(400, errorResponse.getStatus());
    }

    @Test
    void findById_shouldReturnBookingResponseSuccess() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/bookings/{bookingId}", NORMAL_BOOKING_ID)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        BookingResponse bookingResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), BookingResponse.class);

        assertThat(bookingResponse).isNotNull();
        assertThat(bookingResponse.getId()).isEqualTo(NORMAL_BOOKING_ID);
        assertThat(bookingResponse.getRoomId()).isEqualTo(CANCELLING_ROOM.getId());
        assertThat(bookingResponse.getStartTime()).isEqualTo(LocalDateTime.of(2026, 1, 20, 14, 0));
        assertThat(bookingResponse.getEndTime()).isEqualTo(LocalDateTime.of(2026, 1, 20, 15, 0));
        assertThat(bookingResponse.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void findById_shouldReturnBookingResponseNotFound() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/bookings/{bookingId}", -999L)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse errorResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertEquals("BOOKING_NOT_FOUND", errorResponse.getError());
        assertEquals("Booking not found", errorResponse.getMessage());
        assertEquals(404, errorResponse.getStatus());
    }

    @Test
    void findById_shouldReturnBookingResponseSuccessForAdmin() throws Exception {

        LoginRequest loginRequest =
                LoginRequest.builder().username("admin1").password("123").build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JwtResponse jwtResponse =
                objectMapper.readValue(loginResult.getResponse().getContentAsString(), JwtResponse.class);
        jwtToken = jwtResponse.getToken();

        // запрос от админа
        MvcResult result = mockMvc.perform(get("/api/bookings/{bookingId}", NORMAL_BOOKING_ID)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        BookingResponse bookingResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), BookingResponse.class);

        assertThat(bookingResponse).isNotNull();
        assertThat(bookingResponse.getId()).isEqualTo(NORMAL_BOOKING_ID);
        assertThat(bookingResponse.getRoomId()).isEqualTo(CANCELLING_ROOM.getId());
        assertThat(bookingResponse.getStartTime()).isEqualTo(LocalDateTime.of(2026, 1, 20, 14, 0));
        assertThat(bookingResponse.getEndTime()).isEqualTo(LocalDateTime.of(2026, 1, 20, 15, 0));
        assertThat(bookingResponse.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }
}

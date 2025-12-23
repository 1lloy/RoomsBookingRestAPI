package com.illoy.roombooking.integration.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.illoy.roombooking.database.entity.*;
import com.illoy.roombooking.database.repository.BookingRepository;
import com.illoy.roombooking.database.repository.RoomRepository;
import com.illoy.roombooking.database.repository.UserRepository;
import com.illoy.roombooking.dto.request.LoginRequest;
import com.illoy.roombooking.dto.request.RoomCreateEditRequest;
import com.illoy.roombooking.dto.response.JwtResponse;
import com.illoy.roombooking.dto.response.RoomResponse;
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

public class AdminRoomControllerTest extends IntegrationTestBase {

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

    private Long ROOM_ID;
    private Room ROOM;
    private User USER;

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

        USER = user2;

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

        ROOM_ID = room2.getId();
        ROOM = room2;

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
                .startTime(LocalDateTime.of(2025, 1, 20, 10, 0))
                .endTime(LocalDateTime.of(2025, 1, 20, 11, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking booking3 = Booking.builder()
                .room(room1)
                .user(user3)
                .startTime(LocalDateTime.of(2025, 1, 20, 11, 0))
                .endTime(LocalDateTime.of(2025, 1, 20, 12, 30))
                .status(BookingStatus.CANCELLED)
                .build();

        Booking booking4 = Booking.builder()
                .room(room3)
                .user(user2)
                .startTime(LocalDateTime.of(2025, 1, 20, 14, 0))
                .endTime(LocalDateTime.of(2025, 1, 20, 15, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking booking5 = Booking.builder()
                .room(room3)
                .user(user2)
                .startTime(LocalDateTime.of(2025, 1, 22, 15, 30))
                .endTime(LocalDateTime.of(2025, 1, 22, 17, 0))
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
    void findAll_shouldReturnRoomResponse() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/rooms")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        List<RoomResponse> rooms =
                objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

        assertThat(rooms).hasSize(4);
        assertThat(rooms).anyMatch(room -> !room.isActive());
    }

    @Test
    void findById_shouldReturnRoomResponseSuccess() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/rooms/{roomId}", ROOM_ID)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        RoomResponse room = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

        assertEquals("Meeting Room B", room.getName());
        assertEquals(ROOM_ID, room.getId());
        assertEquals(10, room.getCapacity());
        assertTrue(room.isActive());
    }

    @Test
    void findById_shouldReturnRoomNotFoundException() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/rooms/{roomId}", -999L)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        // then
        assertEquals("ROOM_NOT_FOUND", response.getError());
        assertEquals("Room not found or inactive with id: -999", response.getMessage());
        assertEquals(404, response.getStatus());
        assertThat(response.getCertainErrors()).isNullOrEmpty();
    }

    @Test
    void create_shouldReturnRoomResponseSuccess() throws Exception {

        RoomCreateEditRequest request =
                RoomCreateEditRequest.builder().name("New Room G").capacity(15).build();

        MvcResult result = mockMvc.perform(post("/api/admin/rooms")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        RoomResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), RoomResponse.class);

        assertEquals(request.getName(), response.getName());
        assertEquals(request.getCapacity(), response.getCapacity());
    }

    @Test
    void update_shouldReturnRoomResponseSuccess() throws Exception {

        RoomCreateEditRequest request = RoomCreateEditRequest.builder()
                .name("Meeting Room B-1")
                .capacity(15)
                .build();

        MvcResult result = mockMvc.perform(put("/api/admin/rooms/{roomId}", ROOM_ID)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        RoomResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), RoomResponse.class);

        assertEquals(request.getName(), response.getName());
        assertEquals(ROOM_ID, response.getId());
        assertEquals(request.getCapacity(), response.getCapacity());
        assertTrue(response.isActive());
    }

    @Test
    void update_shouldReturnRoomNotFoundException() throws Exception {

        RoomCreateEditRequest request = RoomCreateEditRequest.builder()
                .name("Meeting Room B-1")
                .capacity(15)
                .build();

        MvcResult result = mockMvc.perform(put("/api/admin/rooms/{roomId}", -999L)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        // then
        assertEquals("ROOM_NOT_FOUND", response.getError());
        assertEquals("Room not found or inactive with id: -999", response.getMessage());
        assertEquals(404, response.getStatus());
        assertThat(response.getCertainErrors()).isNullOrEmpty();
    }

    @Test
    void update_shouldReturnRoomAlreadyExistsException() throws Exception {

        RoomCreateEditRequest request =
                RoomCreateEditRequest.builder().name("Small Room C").capacity(5).build();

        MvcResult result = mockMvc.perform(put("/api/admin/rooms/{roomId}", ROOM_ID)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        // then
        assertEquals("ROOM_ALREADY_EXISTS", response.getError());
        assertEquals("Room with name already exists: " + request.getName(), response.getMessage());
        assertEquals(400, response.getStatus());
        assertThat(response.getCertainErrors()).isNullOrEmpty();
    }

    @Test
    void updateStatus_shouldReturnNoContentSuccess() throws Exception {
        mockMvc.perform(patch("/api/admin/rooms/{roomId}/status", ROOM_ID)
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("newStatus", "false")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andReturn();
    }

    @Test
    void updateStatus_shouldReturnRoomNotFoundException() throws Exception {
        MvcResult result = mockMvc.perform(patch("/api/admin/rooms/{roomId}/status", -999L)
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("newStatus", "false")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        // then
        assertEquals("ROOM_NOT_FOUND", response.getError());
        assertEquals("Room not found or inactive with id: -999", response.getMessage());
        assertEquals(404, response.getStatus());
        assertThat(response.getCertainErrors()).isNullOrEmpty();
    }

    @Test
    void updateStatus_shouldReturnRoomStatusConflictException() throws Exception {
        MvcResult result = mockMvc.perform(patch("/api/admin/rooms/{roomId}/status", ROOM_ID)
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("newStatus", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        // then
        assertEquals("ROOM_STATUS_CONFLICT", response.getError());
        assertEquals("Room already has status: active", response.getMessage());
        assertEquals(400, response.getStatus());
        assertThat(response.getCertainErrors()).isNullOrEmpty();
    }

    @Test
    void updateStatus_shouldReturnRoomHasActiveBookingsException() throws Exception {

        Booking booking6 = Booking.builder()
                .room(ROOM)
                .user(USER)
                .startTime(LocalDateTime.of(2100, 1, 20, 10, 0))
                .endTime(LocalDateTime.of(2100, 1, 20, 11, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        bookingRepository.save(booking6);

        MvcResult result = mockMvc.perform(patch("/api/admin/rooms/{roomId}/status", ROOM_ID)
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("newStatus", "false")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        ErrorResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        // then
        assertEquals("ROOM_BOOKINGS_CONFLICT", response.getError());
        assertEquals("Cannot delete room with active bookings. Cancel bookings first.", response.getMessage());
        assertEquals(400, response.getStatus());
        assertThat(response.getCertainErrors()).isNullOrEmpty();
    }
}

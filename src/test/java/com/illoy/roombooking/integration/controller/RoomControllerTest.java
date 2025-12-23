package com.illoy.roombooking.integration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.illoy.roombooking.dto.response.JwtResponse;
import com.illoy.roombooking.dto.response.RoomAvailabilityResponse;
import com.illoy.roombooking.dto.response.RoomResponse;
import com.illoy.roombooking.exception.ErrorResponse;
import com.illoy.roombooking.integration.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public class RoomControllerTest extends IntegrationTestBase {

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
    private Long AVAILABLE_ROOM_ID;

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

        Room room1 = Room.builder().name("A Room").capacity(10).isActive(true).build();
        Room room2 = Room.builder().name("B Room").capacity(8).isActive(false).build();
        Room room3 = Room.builder().name("C Room").capacity(20).isActive(true).build();

        roomRepository.saveAll(List.of(room1, room2, room3));

        ACTIVE_ROOM_ID = room3.getId();
        INACTIVE_ROOM_ID = room2.getId();
        AVAILABLE_ROOM_ID = room1.getId();

        Booking booking1 = Booking.builder()
                .room(room1)
                .user(user)
                .startTime(LocalDateTime.of(2026, 2, 10, 10, 0))
                .endTime(LocalDateTime.of(2026, 2, 10, 11, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking booking2 = Booking.builder()
                .room(room1)
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
    void findAllActiveList_shouldReturnOnlyActiveRooms() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/rooms")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        List<RoomResponse> rooms =
                objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

        assertThat(rooms).isNotEmpty();
        assertThat(rooms).hasSize(2);
        assertThat(rooms).allMatch(RoomResponse::isActive);
        assertThat(rooms).extracting(RoomResponse::getName).containsExactlyInAnyOrder("A Room", "C Room");

        assertThat(rooms).extracting(RoomResponse::getName).doesNotContain("B Room");
    }

    @Test
    void findAllActivePage_shouldReturnPagedActiveRooms() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/rooms/page")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "name")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Преобразуйте JSON в строку и используйте JsonPath
        String jsonResponse = result.getResponse().getContentAsString();

        // Детальные проверки с AssertJ
        List<String> roomNames = JsonPath.read(jsonResponse, "$.content[*].name");
        List<Boolean> activeStatuses = JsonPath.read(jsonResponse, "$.content[*].active");

        assertThat(roomNames).isSorted();
        assertThat(roomNames).hasSize(2);
        assertThat(roomNames).doesNotContain("B Room");
        assertThat(activeStatuses).containsOnly(true);
    }

    @Test
    void findActiveByCapacity_shouldReturnActiveRoomsMeetingMinCapacity() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/rooms/available")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("minCapacity", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        List<RoomResponse> rooms =
                objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

        assertThat(rooms).hasSize(2);
        assertThat(rooms).extracting(RoomResponse::getName).containsExactlyInAnyOrder("A Room", "C Room");

        assertThat(rooms).allMatch(r -> r.getCapacity() >= 10);

        assertThat(rooms).allMatch(RoomResponse::isActive);
    }

    @Test
    void findActiveBySearchTerm_shouldReturnRoomsContainingSearchTermIgnoringCase() throws Exception {

        MvcResult result = mockMvc.perform(get("/api/rooms/search")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("searchTerm", "room")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        List<RoomResponse> rooms =
                objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

        assertThat(rooms).hasSize(2);

        assertThat(rooms).extracting(RoomResponse::getName).containsExactlyInAnyOrder("A Room", "C Room");

        assertThat(rooms).allMatch(RoomResponse::isActive);
    }

    @Test
    void findActiveBySearchTerm_shouldUseDefaultValue_whenParamMissing() throws Exception {

        MvcResult result = mockMvc.perform(get("/api/rooms/search")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        List<RoomResponse> rooms =
                objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

        assertThat(rooms).hasSize(2);

        assertThat(rooms).extracting(RoomResponse::getName).containsExactlyInAnyOrder("A Room", "C Room");
    }

    @Test
    void findActiveById_shouldReturnRoom_whenRoomIsActive() throws Exception {

        MvcResult result = mockMvc.perform(get("/api/rooms/{id}", ACTIVE_ROOM_ID)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        RoomResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), RoomResponse.class);

        assertThat(response.getId()).isEqualTo(ACTIVE_ROOM_ID);
        assertThat(response.getName()).isEqualTo("C Room");
        assertThat(response.getCapacity()).isEqualTo(20);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void findActiveById_shouldReturn404_whenRoomNotFound() throws Exception {

        Long nonExistingId = -999L;

        MvcResult result = mockMvc.perform(get("/api/rooms/{id}", nonExistingId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse errorResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        // then
        assertEquals("ROOM_NOT_FOUND", errorResponse.getError());
        assertEquals("Room not found or inactive with id: " + nonExistingId, errorResponse.getMessage());
        assertEquals(404, errorResponse.getStatus());
    }

    @Test
    void findActiveById_shouldReturn404_whenRoomIsInactive() throws Exception {

        MvcResult result = mockMvc.perform(get("/api/rooms/{id}", INACTIVE_ROOM_ID)
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse errorResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        // then
        assertEquals("ROOM_NOT_FOUND", errorResponse.getError());
        assertEquals("Room not found or inactive with id: " + INACTIVE_ROOM_ID, errorResponse.getMessage());
        assertEquals(404, errorResponse.getStatus());
    }

    @Test
    void checkAvailability_shouldReturnAvailableTrue() throws Exception {
        String start = "2026-02-12T12:00:00";
        String end = "2026-02-12T13:00:00";

        MvcResult mvcResult = mockMvc.perform(get("/api/rooms/{roomId}/availability", AVAILABLE_ROOM_ID)
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("startTime", start)
                        .param("endTime", end))
                .andExpect(status().isOk())
                .andReturn();

        RoomAvailabilityResponse response =
                objectMapper.readValue(mvcResult.getResponse().getContentAsString(), RoomAvailabilityResponse.class);

        assertThat(response).isNotNull();
        assertTrue(response.isAvailableForRequestedTime());
        assertEquals(LocalDate.of(2026, 2, 12), response.getDate());
        assertThat(response.getBusySlots()).isEmpty();
    }

    @Test
    void checkAvailability_shouldReturn404_whenRoomNotFound() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/rooms/{roomId}/availability", -999L)
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("startTime", "2026-02-12T12:00:00")
                        .param("endTime", "2026-02-12T13:00:00"))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse errorResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        // then
        assertEquals("ROOM_NOT_FOUND", errorResponse.getError());
        assertEquals("Room not found or inactive with id: -999", errorResponse.getMessage());
        assertEquals(404, errorResponse.getStatus());
    }

    @Test
    void checkAvailability_shouldReturn404_whenRoomIsInactive() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/rooms/{roomId}/availability", INACTIVE_ROOM_ID)
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("startTime", "2026-02-12T12:00:00")
                        .param("endTime", "2026-02-12T13:00:00"))
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse errorResponse =
                objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        // then
        assertEquals("ROOM_NOT_FOUND", errorResponse.getError());
        assertEquals("Room not found or inactive with id: " + INACTIVE_ROOM_ID, errorResponse.getMessage());
        assertEquals(404, errorResponse.getStatus());
    }
}

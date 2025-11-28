package com.illoy.roombooking.integration.controller.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.illoy.roombooking.database.entity.*;
import com.illoy.roombooking.database.repository.BookingRepository;
import com.illoy.roombooking.database.repository.RoomRepository;
import com.illoy.roombooking.database.repository.UserRepository;
import com.illoy.roombooking.dto.request.LoginRequest;
import com.illoy.roombooking.dto.response.JwtResponse;
import com.illoy.roombooking.integration.IntegrationTestBase;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
@AutoConfigureMockMvc
public class AnalyticsControllerTest extends IntegrationTestBase {

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    private String jwtToken;
    private String USER_2_NAME;
    private String USER_3_NAME;

    @BeforeEach
    void setup() throws Exception {
        User user2 = User.builder().username("anna").email("anna@gmail.com").password(passwordEncoder.encode("123")).role(UserRole.ROLE_USER).isActive(true).build();
        User user3 = User.builder().username("oleg").email("oleg@gmail.com").password(passwordEncoder.encode("123")).role(UserRole.ROLE_USER).isActive(true).build();
        User user4 = User.builder().username("nikol").email("kolya@gmail.com").password(passwordEncoder.encode("123")).role(UserRole.ROLE_USER).isActive(false).build();
        User admin = User.builder().username("admin1").email("mark@gmail.com").password(passwordEncoder.encode("123")).role(UserRole.ROLE_ADMIN).isActive(true).build();

        USER_2_NAME = user2.getUsername();
        USER_3_NAME = user3.getUsername();

        userRepository.saveAll(List.of(user2, user3, user4, admin));

        Room room1 = Room.builder().name("Conference Room A").capacity(20).isActive(true).build();
        Room room2 = Room.builder().name("Meeting Room B").capacity(10).isActive(true).build();
        Room room3 = Room.builder().name("Small Room C").capacity(2).isActive(true).build();
        Room room4 = Room.builder().name("Training Room D").capacity(15).isActive(false).build();

        roomRepository.saveAll(List.of(room1, room2, room3, room4));

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
    void getActiveUsersCount() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/stats/active-users-count")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        Map<String, Long> responseMap = objectMapper.readValue(jsonResponse, new TypeReference<>(){});

        assertEquals(1, responseMap.size());
        assertEquals(2, responseMap.get("activeUsersCount"));
    }

    @Test
    void getActiveRoomsCount() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/stats/active-rooms-count")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        Map<String, Long> responseMap = objectMapper.readValue(jsonResponse, new TypeReference<>(){});

        assertEquals(1, responseMap.size());
        assertEquals(3, responseMap.get("activeRoomsCount"));
    }

    @Test
    void getBookingsCountFromPeriod() throws Exception {
        LocalDateTime startTime = LocalDateTime.of(2100, 1, 20, 11, 0);
        LocalDateTime endTime = LocalDateTime.of(2100, 1, 23, 12, 0);

        MvcResult result = mockMvc.perform(get("/api/admin/stats/bookings-count")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("start", String.valueOf(startTime))
                        .param("end", String.valueOf(endTime)))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        Map<String, Long> responseMap = objectMapper.readValue(jsonResponse, new TypeReference<>(){});

        assertEquals(1, responseMap.size());
        assertEquals(3, responseMap.get("bookingsCount"));
    }

    @Test
    void getBookingsCountFromPeriodByStatus() throws Exception {
        LocalDateTime startTime = LocalDateTime.of(2100, 1, 20, 11, 0);
        LocalDateTime endTime = LocalDateTime.of(2100, 1, 23, 12, 0);

        MvcResult result = mockMvc.perform(get("/api/admin/stats/bookings-count-by-status")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("start", String.valueOf(startTime))
                        .param("end", String.valueOf(endTime)))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        Map<String, Long> responseMap = objectMapper.readValue(jsonResponse, new TypeReference<>(){});

        assertEquals(2, responseMap.size());
        assertEquals(1, responseMap.get("CANCELLED"));
        assertEquals(2, responseMap.get("CONFIRMED"));
    }

    @Test
    void getPopularRooms() throws Exception {
        LocalDateTime startTime = LocalDateTime.of(2100, 1, 20, 11, 0);
        LocalDateTime endTime = LocalDateTime.of(2100, 1, 23, 12, 0);

        MvcResult result = mockMvc.perform(get("/api/admin/stats/popular-rooms")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("start", String.valueOf(startTime))
                        .param("end", String.valueOf(endTime))
                        .param("limit", String.valueOf(2)))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        Map<String, Long> responseMap = objectMapper.readValue(jsonResponse, new TypeReference<>(){});

        assertEquals(2, responseMap.size());
        assertEquals(2, responseMap.get("Small Room C"));
        assertEquals(1, responseMap.get("Conference Room A"));
    }

    @Test
    void getUsersBookingsCount() throws Exception {
        LocalDateTime startTime = LocalDateTime.of(1900, 1, 20, 11, 0);
        LocalDateTime endTime = LocalDateTime.of(2100, 1, 23, 12, 0);

        MvcResult result = mockMvc.perform(get("/api/admin/stats/users-bookings-count")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("start", String.valueOf(startTime))
                        .param("end", String.valueOf(endTime)))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        Map<String, Long> responseMap = objectMapper.readValue(jsonResponse, new TypeReference<>(){});

        assertEquals(2, responseMap.size());
        assertEquals(3, responseMap.get(USER_2_NAME));
        assertEquals(2, responseMap.get(USER_3_NAME));
    }

    @Test
    void getBookingsCountGroupByDow() throws Exception {
        LocalDateTime startTime = LocalDateTime.of(1900, 1, 20, 11, 0);
        LocalDateTime endTime = LocalDateTime.of(2100, 1, 23, 12, 0);

        MvcResult result = mockMvc.perform(get("/api/admin/stats/bookings-count-by-dow")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("start", String.valueOf(startTime))
                        .param("end", String.valueOf(endTime)))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        Map<String, Long> responseMap = objectMapper.readValue(jsonResponse, new TypeReference<>(){});

        assertEquals(3, responseMap.size());
        assertEquals(1, responseMap.get("Monday"));
        assertEquals(3, responseMap.get("Wednesday"));
        assertEquals(1, responseMap.get("Friday"));
    }
}

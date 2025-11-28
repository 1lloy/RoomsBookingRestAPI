package com.illoy.roombooking.integration.service;

import com.illoy.roombooking.database.entity.*;
import com.illoy.roombooking.database.repository.BookingRepository;
import com.illoy.roombooking.database.repository.RoomRepository;
import com.illoy.roombooking.database.repository.UserRepository;
import com.illoy.roombooking.dto.request.RoomCreateEditRequest;
import com.illoy.roombooking.dto.response.RoomAvailabilityResponse;
import com.illoy.roombooking.dto.response.RoomResponse;
import com.illoy.roombooking.exception.RoomAlreadyExistsException;
import com.illoy.roombooking.exception.RoomHasActiveBookingsException;
import com.illoy.roombooking.exception.RoomNotFoundException;
import com.illoy.roombooking.exception.RoomStatusConflictException;
import com.illoy.roombooking.integration.IntegrationTestBase;
import com.illoy.roombooking.service.RoomService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@RequiredArgsConstructor
public class RoomServiceTest extends IntegrationTestBase {

    private final RoomService roomService;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private static Long ACTIVE_ID;
    private static Long INACTIVE_ID;
    private static Room room1;
    private static User user2;

    private final LocalDateTime START = LocalDateTime.of(2024, 1, 20, 13, 0);
    private final LocalDateTime END = LocalDateTime.of(2024, 1, 20, 13, 30);

    @BeforeEach
    void setUp() {
        user2 = User.builder().username("anna").email("anna@gmail.com").password("123").role(UserRole.ROLE_USER).isActive(true).build();
        User user3 = User.builder().username("oleg").email("oleg@gmail.com").password("123").role(UserRole.ROLE_USER).isActive(false).build();

        userRepository.saveAll(List.of(user2, user3));

        room1 = Room.builder().name("Conference Room A").capacity(20).isActive(true).build();
        Room room2 = Room.builder().name("Meeting Room B").capacity(10).isActive(true).build();
        Room room3 = Room.builder().name("Small Room C").capacity(2).isActive(true).build();
        Room room4 = Room.builder().name("Training Room D").capacity(15).isActive(false).build();

        roomRepository.saveAll(List.of(room1, room2, room3, room4));

        ACTIVE_ID = room1.getId();
        INACTIVE_ID = room4.getId();

        Booking booking1 = Booking.builder().room(room1).user(user2)
                .startTime(LocalDateTime.of(2024, 1,20, 9, 0))
                .endTime(LocalDateTime.of(2024, 1,20, 10, 30))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking booking2 = Booking.builder().room(room2).user(user3)
                .startTime(LocalDateTime.of(2024, 1,20, 10, 0))
                .endTime(LocalDateTime.of(2024, 1,20, 11, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking booking3 = Booking.builder().room(room1).user(user3)
                .startTime(LocalDateTime.of(2024, 1,20, 11, 0))
                .endTime(LocalDateTime.of(2024, 1,20, 12, 30))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking booking4 = Booking.builder().room(room3).user(user2)
                .startTime(LocalDateTime.of(2024, 1,20, 14, 0))
                .endTime(LocalDateTime.of(2024, 1,20, 15, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking booking5 = Booking.builder().room(room3).user(user2)
                .startTime(LocalDateTime.of(2024, 1,20, 15, 30))
                .endTime(LocalDateTime.of(2024, 1,20, 17, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        bookingRepository.saveAll(List.of(booking1, booking2, booking3, booking4, booking5));
    }

    @Test
    void findAllActive_shouldReturnOnlyActiveList(){
        List<RoomResponse> rooms = roomService.findAllActive();

        assertThat(rooms).hasSize(3);
        assertThat(rooms).noneMatch(room -> !room.isActive());
    }

    @Test
    void findAllActive_shouldReturnOnlyActivePage(){

        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());

        // when
        Page<RoomResponse> result = roomService.findAllActive(pageable);

        // then
        assertEquals(3, result.getTotalElements());
        assertThat(result).extracting(RoomResponse::getName).containsExactly("Conference Room A", "Meeting Room B", "Small Room C");
        assertThat(result).noneMatch(userResponse -> !userResponse.isActive());
    }

    @Test
    void findActiveById_shouldReturnOnlyActiveSuccessfully(){
        RoomResponse response = roomService.findActiveRoomById(ACTIVE_ID);
        assertEquals(ACTIVE_ID, response.getId());
        assertEquals(20, response.getCapacity());
    }

    @Test
    void findActiveById_shouldThrowExceptionWithInactiveRoom(){
        assertThatThrownBy(() -> roomService.findActiveRoomById(INACTIVE_ID))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessageContaining(String.valueOf(INACTIVE_ID));
    }

    @Test
    void findActiveByMinCapacity_shouldReturnActiveWithGreaterCapacity(){
        List<RoomResponse> rooms = roomService.findActiveByCapacity(10);

        assertThat(rooms).hasSize(2);
        assertThat(rooms).noneMatch(room -> !room.isActive());
        assertThat(rooms).noneMatch(room -> room.getCapacity() < 10);
    }

    @Test
    void countActive_shouldReturnCountActive(){
        Long count = roomService.countActiveRooms();

        assertThat(count).isEqualTo(3);
    }

    @Test
    void searchByName_shouldReturnActiveByNameTerm(){
        List<RoomResponse> rooms = roomService.searchActiveByName("oom");

        assertThat(rooms).hasSize(3);
        assertThat(rooms).noneMatch(room -> !room.isActive());
    }

    @Test
    void checkAvailability_shouldReturnRoomSheduleSuccessfully(){
        RoomAvailabilityResponse response = roomService.checkAvailability(ACTIVE_ID, START, END);

        // then
        assertThat(response.isAvailableForRequestedTime()).isTrue();
        assertThat(response.getBusySlots()).hasSize(2);
        assertThat(response.getDate()).isEqualTo(START.toLocalDate());
    }

    @Test
    void checkAvailability_shouldReturnRoomSheduleFailure(){
        LocalDateTime START_FAILED = LocalDateTime.of(2024, 1, 20, 9, 30);
        LocalDateTime END_FAILED = LocalDateTime.of(2024, 1, 20, 11, 0);

        RoomAvailabilityResponse response = roomService.checkAvailability(ACTIVE_ID, START_FAILED, END_FAILED);

        // then
        assertThat(response.isAvailableForRequestedTime()).isFalse();
        assertThat(response.getBusySlots()).hasSize(2);
        assertThat(response.getDate()).isEqualTo(START.toLocalDate());
    }

    @Test
    void create_shouldReturnRoomResponseSuccessfully(){
        // given
        RoomCreateEditRequest request = RoomCreateEditRequest.builder().name("Meeting Room B-1").capacity(20).build();

        //when
        RoomResponse roomResponse = roomService.create(request);

        // then
        assertThat(roomResponse.getName()).isEqualTo(request.getName());
        assertThat(roomResponse.getCapacity()).isEqualTo(request.getCapacity());
        assertThat(roomResponse.isActive()).isTrue();
    }

    @Test
    void create_shouldReturnRoomResponseFailure(){
        // given
        RoomCreateEditRequest request = RoomCreateEditRequest.builder().name("Meeting Room B").capacity(10).build();

        // expect
        assertThatThrownBy(() -> roomService.create(request))
                .isInstanceOf(RoomAlreadyExistsException.class)
                .hasMessageContaining("Meeting Room B");
    }

    @Test
    void update_shouldReturnRoomResponseSuccessfully(){
        // given
        RoomCreateEditRequest request = RoomCreateEditRequest.builder().name("Conference Room A").description("Super Room").capacity(25).build();

        //when
        RoomResponse roomResponse = roomService.update(ACTIVE_ID, request);

        // then
        assertEquals(ACTIVE_ID, roomResponse.getId());
        assertEquals(request.getName(), roomResponse.getName());
        assertEquals(request.getCapacity(), roomResponse.getCapacity());
    }

    @Test
    void update_shouldReturnRoomResponseNotFound(){
        // given
        RoomCreateEditRequest request = RoomCreateEditRequest.builder().name("Conference Room Z").capacity(40).build();

        // then
        assertThatThrownBy(() -> roomService.update(-999L, request))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessageContaining(String.valueOf(-999L));
    }

    @Test
    void update_shouldReturnRoomResponseAlreadyExists(){
        // given
        RoomCreateEditRequest request = RoomCreateEditRequest.builder().name("Meeting Room B").build();

        // then
        assertThatThrownBy(() -> roomService.update(ACTIVE_ID, request))
                .isInstanceOf(RoomAlreadyExistsException.class)
                .hasMessageContaining("Meeting Room B");
    }

    @Test
    void updateStatus_shouldUpdateStatusSuccessfully(){
        // when
        roomService.updateRoomStatus(ACTIVE_ID, false);

        // очистка persistence context перед тем, как взять обновленную сущность из базы
        entityManager.clear();

        RoomResponse response = roomService.findById(ACTIVE_ID);

        // then
        assertEquals(ACTIVE_ID, response.getId());
        assertFalse(response.isActive());
    }

    @Test
    void updateStatus_shouldUpdateStatusNotFound(){
        // then
        assertThatThrownBy(() -> roomService.updateRoomStatus(-999L, false))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessageContaining(String.valueOf(-999L));
    }

    @Test
    void updateStatus_shouldUpdateStatusSameStatus(){
        // then
        assertThatThrownBy(() -> roomService.updateRoomStatus(ACTIVE_ID, true))
                .isInstanceOf(RoomStatusConflictException.class)
                .hasMessageContaining("Room already has status");
    }

    @Test
    void updateStatus_shouldUpdateStatusHasBookings(){
        //given
        Booking booking6 = Booking.builder().room(room1).user(user2)
                .startTime(LocalDateTime.of(2030, 12,2, 11, 30))
                .endTime(LocalDateTime.of(2030, 12,2, 12, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        bookingRepository.saveAndFlush(booking6);

        // then
        assertThatThrownBy(() -> roomService.updateRoomStatus(ACTIVE_ID, false))
                .isInstanceOf(RoomHasActiveBookingsException.class)
                .hasMessageContaining("Cannot delete room");
    }
}

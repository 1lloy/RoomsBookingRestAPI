package com.illoy.roombooking.integration.service;

import com.illoy.roombooking.database.entity.*;
import com.illoy.roombooking.database.repository.BookingRepository;
import com.illoy.roombooking.database.repository.RoomRepository;
import com.illoy.roombooking.database.repository.UserRepository;
import com.illoy.roombooking.dto.request.BookingCreateRequest;
import com.illoy.roombooking.dto.response.BookingResponse;
import com.illoy.roombooking.dto.response.UserResponse;
import com.illoy.roombooking.exception.*;
import com.illoy.roombooking.integration.IntegrationTestBase;
import com.illoy.roombooking.service.BookingService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiredArgsConstructor
public class BookingServiceTest extends IntegrationTestBase {

    @Autowired
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    private static Long WRONG_OWNER_BOOKING_ID;
    private static Long CANCELLED_BOOKING_ID;
    private static Long PAST_BOOKING_ID;
    private static Long USER_ID;
    private static Long ACTIVE_ROOM_ID;
    private static Long INACTIVE_ROOM_ID;
    private static User USER_FOR_CANCELLING;
    private static Room ROOM_FOR_CANCELLING;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        User user2 = User.builder().username("anna").email("anna@gmail.com").password("123").role(UserRole.ROLE_USER).isActive(true).build();
        User user3 = User.builder().username("oleg").email("oleg@gmail.com").password("123").role(UserRole.ROLE_USER).isActive(true).build();
        User admin = User.builder().username("admin1").email("mark@gmail.com").password("123").role(UserRole.ROLE_ADMIN).isActive(true).build();

        userRepository.saveAll(List.of(user2, user3, admin));

        Room room1 = Room.builder().name("Conference Room A").capacity(20).isActive(true).build();
        Room room2 = Room.builder().name("Meeting Room B").capacity(10).isActive(true).build();
        Room room3 = Room.builder().name("Small Room C").capacity(2).isActive(true).build();
        Room room4 = Room.builder().name("Training Room D").capacity(15).isActive(false).build();

        roomRepository.saveAll(List.of(room1, room2, room3, room4));

        Booking booking1 = Booking.builder().room(room1).user(user2)
                .startTime(LocalDateTime.of(2025, 1,20, 9, 0))
                .endTime(LocalDateTime.of(2025, 1,20, 10, 30))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking booking2 = Booking.builder().room(room2).user(user3)
                .startTime(LocalDateTime.of(2026, 1,20, 10, 0))
                .endTime(LocalDateTime.of(2026, 1,20, 11, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking booking3 = Booking.builder().room(room1).user(user3)
                .startTime(LocalDateTime.of(2026, 1,20, 11, 0))
                .endTime(LocalDateTime.of(2026, 1,20, 12, 30))
                .status(BookingStatus.CANCELLED)
                .build();

        Booking booking4 = Booking.builder().room(room3).user(user2)
                .startTime(LocalDateTime.of(2026, 1,20, 14, 0))
                .endTime(LocalDateTime.of(2026, 1,20, 15, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        Booking booking5 = Booking.builder().room(room3).user(user2)
                .startTime(LocalDateTime.of(2026, 1,22, 15, 30))
                .endTime(LocalDateTime.of(2026, 1,22, 17, 0))
                .status(BookingStatus.CONFIRMED)
                .build();

        bookingRepository.saveAll(List.of(booking1, booking2, booking3, booking4, booking5));

        USER_ID = user2.getId();
        USER_FOR_CANCELLING = user2;

        ACTIVE_ROOM_ID = room2.getId();
        INACTIVE_ROOM_ID = room4.getId();
        ROOM_FOR_CANCELLING = room2;

        PAST_BOOKING_ID = booking1.getId();
        WRONG_OWNER_BOOKING_ID = booking2.getId();
        CANCELLED_BOOKING_ID = booking3.getId();
    }

    /*
        1) комната не найдена или неактивна - RoomNotFoundException
        2) комната занята - RoomNotAvailableException
        3) невалидно время бронирования - BookingTimeException("End time must be after start time");
                                          BookingTimeException("Cannot book in the past");
                                          BookingTimeException("Minimum booking duration is 30 minutes");
     */
    @Test
    @WithMockUser(username = "anna", roles = {"USER"})
    void create_shouldReturnBookingResponseSuccess(){
        // given
        BookingCreateRequest request = BookingCreateRequest.builder()
                .roomId(ACTIVE_ROOM_ID)
                .startTime(LocalDateTime.of(2026, 1,20, 11, 0))
                .endTime(LocalDateTime.of(2026, 1,20, 12, 0))
                .build();

        // when
        BookingResponse response = bookingService.create(request);

        assertThat(response).isNotNull();
        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        assertEquals("anna", response.getUserName());
        assertEquals(ACTIVE_ROOM_ID, response.getRoomId());
    }

    @Test
    @WithMockUser(username = "anna", roles = {"USER"})
    void create_shouldReturnExceptionRoomNotFound(){
        // given
        BookingCreateRequest request = BookingCreateRequest.builder()
                .roomId(-999L)
                .startTime(LocalDateTime.of(2026, 1,20, 11, 0))
                .endTime(LocalDateTime.of(2026, 1,20, 12, 0))
                .build();

        // when
        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessageContaining("Room not found or inactive");
    }

    @Test
    @WithMockUser(username = "anna", roles = {"USER"})
    void create_shouldReturnExceptionRoomInactive(){
        // given
        BookingCreateRequest request = BookingCreateRequest.builder()
                .roomId(INACTIVE_ROOM_ID)
                .startTime(LocalDateTime.of(2026, 1,20, 11, 0))
                .endTime(LocalDateTime.of(2026, 1,20, 12, 0))
                .build();

        // when
        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessageContaining("Room not found or inactive");
    }

    @Test
    @WithMockUser(username = "anna", roles = {"USER"})
    void create_shouldReturnExceptionRoomNotAvailable(){
        // given
        BookingCreateRequest request = BookingCreateRequest.builder()
                .roomId(ACTIVE_ROOM_ID)
                .startTime(LocalDateTime.of(2026, 1,20, 10, 30))
                .endTime(LocalDateTime.of(2026, 1,20, 11, 30))
                .build();

        // when
        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(RoomNotAvailableException.class)
                .hasMessageContaining("Room is not available for selected time");
    }

    @Test
    @WithMockUser(username = "anna", roles = {"USER"})
    void create_shouldReturnValidationExceptionEndBeforeStart(){
        // given
        BookingCreateRequest request = BookingCreateRequest.builder()
                .roomId(ACTIVE_ROOM_ID)
                .startTime(LocalDateTime.of(2026, 1,20, 13, 30))
                .endTime(LocalDateTime.of(2026, 1,20, 12, 30))
                .build();

        // when
        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(BookingTimeException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    @WithMockUser(username = "anna", roles = {"USER"})
    void create_shouldReturnValidationExceptionPastTime(){
        // given
        BookingCreateRequest request = BookingCreateRequest.builder()
                .roomId(ACTIVE_ROOM_ID)
                .startTime(LocalDateTime.of(2025, 1,20, 12, 30))
                .endTime(LocalDateTime.of(2025, 1,20, 13, 30))
                .build();

        // when
        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(BookingTimeException.class)
                .hasMessageContaining("Cannot book in the past");
    }

    @Test
    @WithMockUser(username = "anna", roles = {"USER"})
    void create_shouldReturnValidationExceptionMinDuration(){
        // given
        BookingCreateRequest request = BookingCreateRequest.builder()
                .roomId(ACTIVE_ROOM_ID)
                .startTime(LocalDateTime.of(2026, 1,20, 12, 30))
                .endTime(LocalDateTime.of(2026, 1,20, 12, 59))
                .build();

        // when
        assertThatThrownBy(() -> bookingService.create(request))
                .isInstanceOf(BookingTimeException.class)
                .hasMessageContaining("Minimum booking duration is 30 minutes");
    }

    /*
        1) бронирование по id не найдено - BookingNotFoundException
        2) чужое бронирование нельзя - AccessDeniedException
        3) админу можно отменить чужое бронирование
        4) нельзя отменить бронирование в прошлом (когда startTime < now())
        5) бронирование уже отменено (статус CANCELLED)
     */
    @Test
    @WithMockUser(username = "anna", roles = {"USER"})
    void cancelUser_shouldReturnBookingResponseSuccess(){
        Booking booking6 = Booking.builder().room(ROOM_FOR_CANCELLING).user(USER_FOR_CANCELLING)
                .startTime(LocalDateTime.of(2026, 1,22, 10, 0))
                .endTime(LocalDateTime.of(2026, 1,22, 11, 0))
                .status(BookingStatus.CONFIRMED)
                .build();
        bookingRepository.save(booking6);

        Long id = booking6.getId();

        // when
        BookingResponse result = bookingService.cancel(id);

        // then
        assertThat(result).isNotNull();
        assertEquals(id, result.getId());
        assertEquals("anna", result.getUserName());
        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        assertEquals(ROOM_FOR_CANCELLING.getId(), result.getRoomId());
    }

    @Test
    @WithMockUser(username = "admin1", roles = {"ADMIN"})
    void cancelAdmin_shouldReturnBookingResponseSuccess(){
        // when
        BookingResponse result = bookingService.cancel(WRONG_OWNER_BOOKING_ID);

        // then
        assertThat(result).isNotNull();
        assertEquals(WRONG_OWNER_BOOKING_ID, result.getId());
        assertEquals("oleg", result.getUserName());
        assertEquals(BookingStatus.CANCELLED, result.getStatus());
    }

    @Test
    @WithMockUser(username = "anna", roles = {"USER"})
    void cancel_shouldReturnBookingNotFoundException(){
        assertThatThrownBy(() -> bookingService.cancel(-999L))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining("Booking not found");
    }

    @Test
    @WithMockUser(username = "anna", roles = {"USER"})
    void cancel_shouldReturnAccessDeniedException(){
        assertThatThrownBy(() -> bookingService.cancel(WRONG_OWNER_BOOKING_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You can only cancel your own bookings");
    }

    @Test
    @WithMockUser(username = "anna", roles = {"USER"})
    void cancel_shouldReturnBookingTimeException(){
        assertThatThrownBy(() -> bookingService.cancel(PAST_BOOKING_ID))
                .isInstanceOf(BookingTimeException.class)
                .hasMessageContaining("Cannot cancel past booking");
    }

    @Test
    @WithMockUser(username = "oleg", roles = {"USER"})
    void cancel_shouldReturnBookingStatusConflictException(){
        assertThatThrownBy(() -> bookingService.cancel(CANCELLED_BOOKING_ID))
                .isInstanceOf(BookingStatusConflictException.class)
                .hasMessageContaining("Booking already cancelled");
    }

    @Test
    void updateStatus_shouldReturnBookingResponseSuccess(){
        // when
        BookingResponse response = bookingService.updateStatus(CANCELLED_BOOKING_ID, BookingStatus.CONFIRMED);

        assertThat(response).isNotNull();
        assertEquals(CANCELLED_BOOKING_ID, response.getId());
        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
    }

    @Test
    void updateStatus_shouldReturnBookingNotFoundException(){
        assertThatThrownBy(() -> bookingService.updateStatus(-999L, BookingStatus.COMPLETED))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining("Booking not found");
    }

    @Test
    void updateStatus_shouldReturnBookingHasSameStatus(){
        assertThatThrownBy(() -> bookingService.updateStatus(CANCELLED_BOOKING_ID, BookingStatus.CANCELLED))
                .isInstanceOf(BookingStatusConflictException.class)
                .hasMessageContaining("Booking already has status: ");
    }

    @Test
    void findAll_shouldReturnAllBookings(){
        List<BookingResponse> bookings = bookingService.findAll();

        assertThat(bookings).hasSize(5);
        assertThat(bookings).anyMatch(booking -> booking.getStatus().equals(BookingStatus.CANCELLED));
    }

    @Test
    @WithMockUser(username = "anna", roles = {"USER"})
    void findById_shouldReturnBookingHasRequiredAuthoritySuccess(){
        BookingResponse response = bookingService.findById(PAST_BOOKING_ID);

        assertEquals("anna", response.getUserName());
        assertEquals("Conference Room A", response.getRoomName());
        assertEquals(LocalDateTime.of(2025, 1, 20, 9,0), response.getStartTime());
    }

    @Test
    @WithMockUser(username = "oleg", roles = {"USER"})
    void findById_shouldReturnBookingHasFalseAuthorityFailure(){
        assertThatThrownBy(() -> bookingService.findById(PAST_BOOKING_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You can only check your own bookings");
    }

    @Test
    @WithMockUser(username = "admin1", roles = {"ADMIN"})
    void findById_shouldReturnBookingHasAdminRoleSuccess(){
        BookingResponse response = bookingService.findById(PAST_BOOKING_ID);

        assertEquals("anna", response.getUserName());
        assertEquals("Conference Room A", response.getRoomName());
        assertEquals(LocalDateTime.of(2025, 1, 20, 9,0), response.getStartTime());
    }

    @Test
    @WithMockUser(username = "admin1", roles = {"ADMIN"})
    void findById_shouldReturnBookingNotFoundException(){
        assertThatThrownBy(() -> bookingService.findById(-999L))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining("Booking not found");
    }

    @Test
    @WithMockUser(username = "anna", roles = {"USER"})
    void findUserBookings_shouldReturnPageWithOnlyDatesSuccess(){
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("startTime").descending());
        LocalDate fromDate = LocalDate.of(2026, 1, 20);
        LocalDate toDate = LocalDate.of(2026, 1, 21);

        Page<BookingResponse> response = bookingService.findUserBookings(pageable, null, fromDate, toDate);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response).extracting(BookingResponse::getRoomName).containsExactly("Small Room C");
        assertThat(response.getContent().getFirst().getStartTime()).isBefore(toDate.atTime(LocalTime.MAX));
        assertThat(response.getContent().getFirst().getStartTime()).isAfter(fromDate.atStartOfDay());
    }

    @Test
    @WithMockUser(username = "anna", roles = {"USER"})
    void findUserBookings_shouldReturnPageWithOnlyStatusSuccess(){
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("startTime").descending());

        // when
        Page<BookingResponse> response = bookingService.findUserBookings(pageable, BookingStatus.CONFIRMED, null, null);

        // then
        assertThat(response.getContent()).hasSize(3);
        assertEquals(LocalDateTime.of(2026, 1,22, 15, 30), response.getContent().getFirst().getStartTime());
        assertThat(response).allMatch(booking -> booking.getStatus().equals(BookingStatus.CONFIRMED));
    }

    @Test
    @WithMockUser(username = "anna", roles = {"USER"})
    void findUserBookings_shouldReturnPageWithNoFiltersSuccess() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("startTime").descending());

        // when
        Page<BookingResponse> response = bookingService.findUserBookings(pageable, null, null, null);

        // then
        assertThat(response.getContent()).hasSize(3);
        assertEquals(LocalDateTime.of(2026, 1,22, 15, 30), response.getContent().getFirst().getStartTime());
    }

    @Test
    void findByStatus_shouldReturnPageWithOneStatusSuccess(){
        // given
        Pageable pageable = PageRequest.of(0, 3, Sort.by("startTime").descending());

        // when
        Page<BookingResponse> response = bookingService.findByStatus(BookingStatus.CONFIRMED, pageable);

        // then
        assertThat(response.getContent()).hasSize(3);
        assertThat(response).noneMatch(booking -> booking.getStatus().equals(BookingStatus.CANCELLED));
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void findCountByPeriod_shouldReturnMapWithStatusesCount(){
        // given
        LocalDateTime start = LocalDateTime.of(2026, 1, 19, 0,0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 23, 0,0);

        // when
        Map<String, Long> result = bookingService.findCountByPeriodGroupByStatus(start, end);

        // then
        assertThat(result.get("CONFIRMED")).isEqualTo(3L);
        assertThat(result.get("CANCELLED")).isEqualTo(1L);
        assertThat(result).hasSize(2);
    }

    @Test
    void countStartTimeBetween_shouldReturnBookingCountByPeriod(){
        // given
        LocalDateTime start = LocalDateTime.of(2026, 1, 19, 0,0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 23, 0,0);

        long result = bookingService.countByStartTimeBetween(start, end);
        assertThat(result).isEqualTo(4L);
    }

    @Test
    void findBookingsCountByDow_shouldReturnMapWithCount(){
        // given
        LocalDateTime start = LocalDateTime.of(2026, 1, 19, 0,0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 23, 0,0);

        Map<String, Long> result = bookingService.findBookingsCountByDow(start, end);

        // then
        assertThat(result.get("Tuesday")).isEqualTo(3L);
        assertThat(result.get("Thursday")).isEqualTo(1L);
        assertThat(result).hasSize(2);
    }

    @Test
    void findPopularRooms_shouldReturnMapWithRoomAndCount(){
        // given
        LocalDateTime start = LocalDateTime.of(2026, 1, 19, 0,0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 23, 0,0);

        Map<String, Long> result = bookingService.findPopularRooms(start, end, 4);

        // then
        assertThat(result.keySet())
                .containsExactlyInAnyOrder("Conference Room A", "Meeting Room B", "Small Room C");

        assertThat(result).hasSize(3);

        Long countRoom1 = result.entrySet()
                .stream()
                .filter(entry -> "Conference Room A".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);

        assertThat(countRoom1).isEqualTo(1L);
    }

    @Test
    void findUserBookingsCount_shouldReturnMapWithUserAndCount(){
        // given
        LocalDateTime start = LocalDateTime.of(2026, 1, 19, 0,0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 23, 0,0);

        // when
        Map<String, Long> result = bookingService.findUsersBookingsCount(start, end);

        // then
        assertThat(result).hasSize(2);

        assertThat(result.keySet()).containsExactly("anna", "oleg");

        Long countUser2 = result.entrySet()
                .stream()
                .filter(entry -> "anna".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);

        assertThat(countUser2).isEqualTo(2L);
    }

    @Test
    void findByUserId_shouldReturnPageForUserSuccess(){
        // given
        Pageable pageable = PageRequest.of(0, 2, Sort.by("startTime").descending());

        Page<BookingResponse> result = bookingService.findByUserId(USER_ID, pageable);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(booking -> booking.getUserId().equals(USER_ID));
        assertThat(result.hasNext()).isTrue();
    }
}

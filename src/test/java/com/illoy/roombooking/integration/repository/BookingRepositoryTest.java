package com.illoy.roombooking.integration.repository;

import com.illoy.roombooking.database.entity.*;
import com.illoy.roombooking.database.repository.BookingRepository;
import com.illoy.roombooking.database.repository.RoomRepository;
import com.illoy.roombooking.database.repository.UserRepository;
import com.illoy.roombooking.integration.IntegrationTestBase;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class BookingRepositoryTest extends IntegrationTestBase {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldFindConflictingBookings() {
        // given: создаём комнату
        Room room = Room.builder().name("Conference A").isActive(true).capacity(10).build();
        room = roomRepository.save(room);

        LocalDateTime now = LocalDateTime.now();

        // бронирования, которые конфликтуют
        Booking booking1 = Booking.builder().room(room).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(1))
                .endTime(now.plusHours(3)).build();
        Booking booking2 = Booking.builder().room(room).status(BookingStatus.PENDING)
                .startTime(now.plusHours(2))
                .endTime(now.plusHours(4)).build();

        // бронирование, которое не конфликтует
        Booking booking3 = Booking.builder().room(room).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(5))
                .endTime(now.plusHours(6))
                .build();

        // бронирование с другим статусом (CANCELLED) — не учитывается
        Booking booking4 = Booking.builder().room(room).status(BookingStatus.CANCELLED)
                .startTime(now.plusHours(1))
                .endTime(now.plusHours(4))
                .build();

        bookingRepository.saveAll(List.of(booking1, booking2, booking3, booking4));
        entityManager.flush();
        entityManager.clear();

        // when: ищем конфликты для интервала 2-3 часа
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                room.getId(),
                now.plusHours(2),
                now.plusHours(3)
        );

        // then: должны найти booking1 и booking2
        assertThat(conflicts)
                .hasSize(2)
                .extracting(Booking::getId)
                .containsExactlyInAnyOrder(booking1.getId(), booking2.getId());
    }

    @Test
    void shouldFindBookingsByUserIdWithPagination() {
        // given: создаём пользователя и комнату
        User user = User.builder().email("user@test.com").username("user1").password("pass").role(UserRole.ROLE_USER).build();
        user = userRepository.save(user);

        Room room = Room.builder().name("Conference Room").isActive(true).capacity(10).build();
        room = roomRepository.save(room);

        LocalDateTime now = LocalDateTime.now();

        Booking booking1 = Booking.builder().room(room).user(user).status(BookingStatus.CONFIRMED).startTime(now.plusHours(1)).endTime(now.plusHours(2)).build();
        Booking booking2 = Booking.builder().room(room).user(user).status(BookingStatus.PENDING).startTime(now.plusHours(3)).endTime(now.plusHours(4)).build();
        Booking booking3 = Booking.builder().room(room).user(user).status(BookingStatus.CONFIRMED).startTime(now.plusHours(5)).endTime(now.plusHours(6)).build();

        bookingRepository.saveAll(List.of(booking1, booking2, booking3));

        // when: извлекаем первую страницу с 2 элементами
        Pageable pageable = PageRequest.of(0, 2);
        Page<Booking> page = bookingRepository.findByUserIdOrderByStartTimeDesc(user.getId(), pageable);

        // then
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent())
                .hasSize(2)
                .extracting(Booking::getStartTime)
                .isSortedAccordingTo(Comparator.reverseOrder()); // DESC

        // проверка на следующую страницу
        Page<Booking> secondPage = bookingRepository.findByUserIdOrderByStartTimeDesc(user.getId(), PageRequest.of(1, 2));
        assertThat(secondPage.getContent()).hasSize(1);
    }

    @Test
    void shouldFindBookingsByUserIdAndStatusWithPagination() {
        // given: создаём пользователя и комнату
        User user = User.builder().email("user@test.com").username("user1").password("pass").role(UserRole.ROLE_USER).build();
        user = userRepository.save(user);

        Room room = Room.builder().name("Conference Room").isActive(true).capacity(10).build();
        room = roomRepository.save(room);

        LocalDateTime now = LocalDateTime.now();

        Booking booking1 = Booking.builder().room(room).user(user).status(BookingStatus.CONFIRMED).startTime(now.plusHours(1)).endTime(now.plusHours(2)).build();
        Booking booking2 = Booking.builder().room(room).user(user).status(BookingStatus.PENDING).startTime(now.plusHours(3)).endTime(now.plusHours(4)).build();
        Booking booking3 = Booking.builder().room(room).user(user).status(BookingStatus.CONFIRMED).startTime(now.plusHours(5)).endTime(now.plusHours(6)).build();

        bookingRepository.saveAll(List.of(booking1, booking2, booking3));

        // when: извлекаем первую страницу с 2 элементами для статуса CONFIRMED
        Pageable pageable = PageRequest.of(0, 2);
        Page<Booking> page = bookingRepository.findByUserIdAndStatus(user.getId(), BookingStatus.CONFIRMED, pageable);

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.getContent())
                .hasSize(2)
                .allMatch(b -> b.getStatus() == BookingStatus.CONFIRMED);

        // проверка на пустой результат для статуса CANCELLED
        Page<Booking> cancelledPage = bookingRepository.findByUserIdAndStatus(user.getId(), BookingStatus.CANCELLED, pageable);
        assertThat(cancelledPage.getContent()).isEmpty();
        assertThat(cancelledPage.getTotalElements()).isZero();
    }

    @Test
    void shouldFindBookingsByUserIdWithinStartTimeRange() {
        // given
        User user = User.builder().email("user@test.com").username("user1").password("pass").role(UserRole.ROLE_USER).build();
        user = userRepository.save(user);

        Room room = Room.builder().name("Conference Room").isActive(true).capacity(10).build();
        room = roomRepository.save(room);

        LocalDateTime now = LocalDateTime.now();

        Booking booking1 = Booking.builder().room(room).user(user).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(1)).endTime(now.plusHours(2)).build();
        Booking booking2 = Booking.builder().room(room).user(user).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(3)).endTime(now.plusHours(4)).build();
        Booking booking3 = Booking.builder().room(room).user(user).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(5)).endTime(now.plusHours(6)).build();

        bookingRepository.saveAll(List.of(booking1, booking2, booking3));

        // when: ищем бронирования с 2-4 часами интервала
        Pageable pageable = PageRequest.of(0, 5);
        Page<Booking> page = bookingRepository.findByUserIdAndStartTimeBetween(
                user.getId(),
                now.plusHours(2),
                now.plusHours(4),
                pageable
        );

        // then
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent())
                .hasSize(1)
                .allMatch(b -> !b.getStartTime().isBefore(now.plusHours(2)) && !b.getStartTime().isAfter(now.plusHours(4)));
    }

    @Test
    void shouldFindBookingsByStatusWithPagination() {
        // given: создаём пользователя и комнату
        User user = User.builder().email("user@test.com").username("user1").password("pass").role(UserRole.ROLE_USER).build();
        user = userRepository.save(user);

        Room room = Room.builder().name("Conference Room").isActive(true).capacity(10).build();
        room = roomRepository.save(room);

        LocalDateTime now = LocalDateTime.now();

        Booking booking1 = Booking.builder().room(room).user(user).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(1)).endTime(now.plusHours(2)).build();
        Booking booking2 = Booking.builder().room(room).user(user).status(BookingStatus.PENDING)
                .startTime(now.plusHours(3)).endTime(now.plusHours(4)).build();
        Booking booking3 = Booking.builder().room(room).user(user).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(5)).endTime(now.plusHours(6)).build();

        bookingRepository.saveAll(List.of(booking1, booking2, booking3));

        // when: извлекаем первую страницу с 2 элементами для статуса CONFIRMED
        Pageable pageable = PageRequest.of(0, 2);
        Page<Booking> page = bookingRepository.findByStatus(BookingStatus.CONFIRMED, pageable);

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .hasSize(2)
                .allMatch(b -> b.getStatus() == BookingStatus.CONFIRMED);

        // проверка следующей страницы
        Page<Booking> secondPage = bookingRepository.findByStatus(BookingStatus.CONFIRMED, PageRequest.of(1, 2));
        assertThat(secondPage.getContent()).isEmpty();
    }

    @Test
    void shouldFindBookingsByRoomIdAndStartTimeBetweenAndStatuses() {
        // given: фиксированная базовая дата для предсказуемости
        LocalDateTime base = LocalDateTime.of(2025, 11, 20, 16, 0);

        User user = User.builder().email("user@test.com").username("user1").password("pass").role(UserRole.ROLE_USER).build();
        user = userRepository.save(user);

        Room room = Room.builder().name("Conference Room").isActive(true).capacity(10).build();
        room = roomRepository.save(room);

        Booking booking1 = Booking.builder().room(room).user(user).status(BookingStatus.CONFIRMED).startTime(base.plusHours(1)).endTime(base.plusHours(2)).build();

        Booking booking2 = Booking.builder().room(room).user(user).status(BookingStatus.PENDING).startTime(base.plusHours(3)).endTime(base.plusHours(4)).build();

        Booking booking3 = Booking.builder().room(room).user(user).status(BookingStatus.CANCELLED).startTime(base.plusHours(5)).endTime(base.plusHours(6)).build();

        bookingRepository.saveAll(List.of(booking1, booking2, booking3));

        // when: ищем бронирования между base+1h и base+4h с статусами CONFIRMED/PENDING
        List<BookingStatus> statuses = List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING);

        List<Booking> result = bookingRepository.findByRoomIdAndStartTimeBetweenAndStatusIn(
                room.getId(),
                base.plusHours(1),
                base.plusHours(4),
                statuses
        );

        // then: ожидаем 2 бронирования, проверяем статусы и диапазон времени
        assertThat(result)
                .hasSize(2);

        // Проверяем, что все бронирования имеют корректные статусы
        assertThat(result)
                .extracting(Booking::getStatus)
                .allMatch(status -> status == BookingStatus.CONFIRMED || status == BookingStatus.PENDING);

        // Проверяем, что все startTime в пределах диапазона
        assertThat(result)
                .extracting(Booking::getStartTime)
                .allSatisfy(start ->
                        assertThat(start)
                                .isAfterOrEqualTo(base.plusHours(1))
                                .isBeforeOrEqualTo(base.plusHours(4))
                );

        // Проверяем, что id бронирований соответствуют booking1 и booking2
        assertThat(result)
                .extracting(Booking::getId)
                .containsExactlyInAnyOrder(booking1.getId(), booking2.getId());
    }

    @Test
    void shouldCountBookingsWithinStartTimeRange() {
        // given
        User user = User.builder().email("user@test.com").username("user1").password("pass").role(UserRole.ROLE_USER).build();
        user = userRepository.save(user);

        Room room = Room.builder().name("Conference Room").isActive(true).capacity(10).build();
        room = roomRepository.save(room);

        LocalDateTime now = LocalDateTime.now();

        Booking booking1 = Booking.builder().room(room).user(user).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(1)).endTime(now.plusHours(2)).build();
        Booking booking2 = Booking.builder().room(room).user(user).status(BookingStatus.PENDING)
                .startTime(now.plusHours(3)).endTime(now.plusHours(4)).build();
        Booking booking3 = Booking.builder().room(room).user(user).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(5)).endTime(now.plusHours(6)).build();

        bookingRepository.saveAll(List.of(booking1, booking2, booking3));

        // when
        long count = bookingRepository.countByStartTimeBetween(now.plusHours(2), now.plusHours(5));

        // then
        assertThat(count).isEqualTo(2); // booking2 и booking3 (startTime 3 и 5)
    }

    @Test
    void shouldFindBookingsGroupedByDayOfWeek() {
        // given
        User user = User.builder().email("user@test.com").username("user1").password("pass").role(UserRole.ROLE_USER).build();
        user = userRepository.save(user);

        Room room = Room.builder().name("Conference Room").isActive(true).capacity(10).build();
        room = roomRepository.save(room);

        LocalDateTime monday = LocalDateTime.of(2025, 11, 17, 10, 0); // Monday
        LocalDateTime tuesday = LocalDateTime.of(2025, 11, 18, 11, 0); // Tuesday

        Booking booking1 = Booking.builder().room(room).user(user).status(BookingStatus.CONFIRMED)
                .startTime(monday).endTime(monday.plusHours(1)).build();
        Booking booking2 = Booking.builder().room(room).user(user).status(BookingStatus.CONFIRMED)
                .startTime(monday.plusHours(2)).endTime(monday.plusHours(3)).build();
        Booking booking3 = Booking.builder().room(room).user(user).status(BookingStatus.CONFIRMED)
                .startTime(tuesday).endTime(tuesday.plusHours(1)).build();

        bookingRepository.saveAll(List.of(booking1, booking2, booking3));

        // when
        LocalDateTime startRange = LocalDateTime.of(2025, 11, 17, 0, 0);
        LocalDateTime endRange = LocalDateTime.of(2025, 11, 19, 0, 0);

        List<Object[]> results = bookingRepository.findBookingsByDayOfWeek(startRange, endRange);

        // then
        assertThat(results).hasSize(2);

        // PostgreSQL EXTRACT(DOW): 0=Sunday, 1=Monday, 2=Tuesday ...
        Object[] mondayResult = results.get(0); // Monday
        Object[] tuesdayResult = results.get(1); // Tuesday

        assertThat(((Number) mondayResult[0]).intValue()).isEqualTo(1); // Monday
        assertThat(((Number) mondayResult[1]).longValue()).isEqualTo(2); // 2 bookings

        assertThat(((Number) tuesdayResult[0]).intValue()).isEqualTo(2); // Tuesday
        assertThat(((Number) tuesdayResult[1]).longValue()).isEqualTo(1); // 1 booking
    }

    @Test
    void shouldFindPopularRoomsWithPagination() {
        // given
        User user = User.builder().email("user@test.com").username("user1").password("pass").role(UserRole.ROLE_USER).build();
        user = userRepository.save(user);

        Room roomA = Room.builder().name("Room A").isActive(true).capacity(10).build();
        Room roomB = Room.builder().name("Room B").isActive(true).capacity(8).build();
        Room roomC = Room.builder().name("Room C").isActive(true).capacity(12).build();
        roomA = roomRepository.save(roomA);
        roomB = roomRepository.save(roomB);
        roomC = roomRepository.save(roomC);

        LocalDateTime now = LocalDateTime.now();

        Booking b1 = Booking.builder().room(roomA).user(user).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(1)).endTime(now.plusHours(2)).build();
        Booking b2 = Booking.builder().room(roomA).user(user).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(3)).endTime(now.plusHours(4)).build();
        Booking b3 = Booking.builder().room(roomB).user(user).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(1)).endTime(now.plusHours(2)).build();
        Booking b4 = Booking.builder().room(roomC).user(user).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(2)).endTime(now.plusHours(3)).build();
        Booking b5 = Booking.builder().room(roomC).user(user).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(3)).endTime(now.plusHours(4)).build();
        Booking b6 = Booking.builder().room(roomC).user(user).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(4)).endTime(now.plusHours(5)).build();

        bookingRepository.saveAll(List.of(b1, b2, b3, b4, b5, b6));

        // when: ищем популярные комнаты за период с пагинацией (top 2)
        LocalDateTime startRange = LocalDateTime.now();
        LocalDateTime endRange = startRange.plusDays(1);
        Pageable pageable = PageRequest.of(0, 2);

        List<Object[]> popularRooms = bookingRepository.findPopularRooms(startRange, endRange, pageable);

        // then
        assertThat(popularRooms).hasSize(2);

        Object[] topRoom = popularRooms.get(0);
        Object[] secondRoom = popularRooms.get(1);

        assertThat(topRoom[0]).isEqualTo("Room C");
        assertThat(((Number) topRoom[1]).longValue()).isEqualTo(3); // 3 бронирования
        assertThat(secondRoom[0]).isEqualTo("Room A");
        assertThat(((Number) secondRoom[1]).longValue()).isEqualTo(2); // 2 бронирования
    }

    @Test
    void shouldFindUsersBookingsCountByBookingCount() {
        // given
        User user1 = User.builder().email("u1@test.com").username("user1").password("pass").role(UserRole.ROLE_USER).build();
        User user2 = User.builder().email("u2@test.com").username("user2").password("pass").role(UserRole.ROLE_USER).build();
        user1 = userRepository.save(user1);
        user2 = userRepository.save(user2);

        Room room = Room.builder().name("Conference Room").isActive(true).capacity(10).build();
        room = roomRepository.save(room);

        LocalDateTime now = LocalDateTime.now();

        Booking b1 = Booking.builder().room(room).user(user1).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(1)).endTime(now.plusHours(2)).build();
        Booking b2 = Booking.builder().room(room).user(user1).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(3)).endTime(now.plusHours(4)).build();
        Booking b3 = Booking.builder().room(room).user(user2).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(2)).endTime(now.plusHours(3)).build();

        bookingRepository.saveAll(List.of(b1, b2, b3));

        // when
        LocalDateTime startRange = LocalDateTime.now();
        LocalDateTime endRange = startRange.plusDays(1);
        List<Object[]> results = bookingRepository.findUsersBookingsCount(startRange, endRange);

        // then
        assertThat(results).hasSize(2);

        Object[] topUser = results.get(0); // user1 username
        Object[] secondUser = results.get(1); // user2 username

        assertThat(topUser[0]).isEqualTo(user1.getUsername());
        assertThat(((Number) topUser[1]).longValue()).isEqualTo(2); // 2 бронирования

        assertThat(secondUser[0]).isEqualTo(user2.getUsername());
        assertThat(((Number) secondUser[1]).longValue()).isEqualTo(1); // 1 бронирование
    }

    @Test
    void shouldGetBookingCountGroupedByStatus() {
        // given
        User user = User.builder().email("user@test.com").username("user1").password("pass").role(UserRole.ROLE_USER).build();
        user = userRepository.save(user);

        Room room = Room.builder().name("Conference Room").isActive(true).capacity(10).build();
        room = roomRepository.save(room);

        LocalDateTime now = LocalDateTime.now();

        Booking b1 = Booking.builder().room(room).user(user).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(1)).endTime(now.plusHours(2)).build();
        Booking b2 = Booking.builder().room(room).user(user).status(BookingStatus.PENDING)
                .startTime(now.plusHours(3)).endTime(now.plusHours(4)).build();
        Booking b3 = Booking.builder().room(room).user(user).status(BookingStatus.CONFIRMED)
                .startTime(now.plusHours(2)).endTime(now.plusHours(3)).build();

        bookingRepository.saveAll(List.of(b1, b2, b3));

        // when
        LocalDateTime startRange = LocalDateTime.now();
        LocalDateTime endRange = startRange.plusDays(1);

        List<Object[]> results = bookingRepository.getCountGroupByStatus(startRange, endRange);

        // then
        assertThat(results).hasSize(2);

        // Найдём статус CONFIRMED
        Object[] confirmed = results.stream()
                .filter(r -> r[0] == BookingStatus.CONFIRMED)
                .findFirst()
                .orElseThrow();

        Object[] pending = results.stream()
                .filter(r -> r[0] == BookingStatus.PENDING)
                .findFirst()
                .orElseThrow();

        assertThat(((Number) confirmed[1]).longValue()).isEqualTo(2);
        assertThat(((Number) pending[1]).longValue()).isEqualTo(1);
    }
}

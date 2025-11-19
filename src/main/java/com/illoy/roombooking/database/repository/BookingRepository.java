package com.illoy.roombooking.database.repository;

import com.illoy.roombooking.database.entity.Booking;
import com.illoy.roombooking.database.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface BookingRepository extends JpaRepository<Booking,Long> {

    // Основные методы для проверки доступности
    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId " +
            "AND b.status IN ('CONFIRMED', 'PENDING') " +
            "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    List<Booking> findConflictingBookings(@Param("roomId") Long roomId,
                                          @Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    // Бронирования пользователя
    List<Booking> findByUserIdOrderByStartTimeDesc(Long userId);

    Page<Booking> findByUserIdOrderByStartTimeDesc(Long userId, Pageable pageable);

    Page<Booking> findByUserIdAndStatus(Long userId, BookingStatus status, Pageable pageable);

    Page<Booking> findByUserIdAndStartTimeBetween(Long userId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Бронирования по статусу
    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    List<Booking> findByStatusAndStartTimeBetween(BookingStatus status,
                                                  LocalDateTime start,
                                                  LocalDateTime end);

    Page<Booking> findByStatusAndStartTimeBetween(BookingStatus status,
                                                  LocalDateTime start,
                                                  LocalDateTime end,
                                                  Pageable pageable);

    List<Booking> findByRoomIdAndStartTimeBetweenAndStatusIn(Long roomId,
                                                             LocalDateTime startTimeAfter,
                                                             LocalDateTime startTimeBefore,
                                                             List<BookingStatus> statuses);

    // Бронирования в определенный период
    List<Booking> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    Page<Booking> findByStartTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Подсчеты для аналитики
    long countByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    long countByStatusAndStartTimeBetween(BookingStatus status, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndStatus(Long userId, BookingStatus status);

    // Активные бронирования (текущие и будущие)
    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND b.endTime > :now " +
            "ORDER BY b.startTime ASC")
    List<Booking> findUpcomingBookings(@Param("now") LocalDateTime now);

    // Просроченные бронирования
    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND b.endTime < :now")
    List<Booking> findExpiredBookings(@Param("now") LocalDateTime now);

    // АНАЛИТИКА ДЛЯ АДМИНКИ
    @Query("SELECT new map(b.room.name as roomName, COUNT(b) as bookingCount) " +
            "FROM Booking b WHERE b.startTime BETWEEN :start AND :end " +
            "GROUP BY b.room.name " +
            "ORDER BY COUNT(b) DESC")
    List<Map<String, Object>> getBookingsCountByRoom(@Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);

    @Query(value = "SELECT EXTRACT(DOW FROM start_time) as day_of_week, COUNT(*) " +
            "FROM bookings WHERE start_time BETWEEN :start AND :end " +
            "GROUP BY EXTRACT(DOW FROM start_time) " +
            "ORDER BY day_of_week",
            nativeQuery = true)
    List<Object[]> findBookingsByDayOfWeek(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (end_time - start_time)) / 3600.0) " +
            "FROM bookings WHERE start_time BETWEEN :start AND :end AND status = 'CONFIRMED'",
            nativeQuery = true)
    Double getAverageBookingDuration(@Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    // Самые популярные комнаты
    @Query("SELECT b.room.name, COUNT(b) " +
            "FROM Booking b WHERE b.startTime BETWEEN :start AND :end " +
            "GROUP BY b.room.name " +
            "ORDER BY COUNT(b) DESC")
    List<Object[]> findPopularRooms(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end,
                                    Pageable pageable);

    // Статистика по пользователям
    @Query("SELECT b.user, COUNT(b) " +
            "FROM Booking b WHERE b.startTime BETWEEN :start AND :end " +
            "GROUP BY b.user " +
            "ORDER BY COUNT(b) DESC")
    List<Object[]> findActiveUsers(@Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    @Query("SELECT b.status, COUNT(b) FROM Booking b " +
            "WHERE b.startTime BETWEEN :start AND :end " +
            "GROUP BY b.status")
    List<Object[]> getCountGroupByStatus(@Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);
}

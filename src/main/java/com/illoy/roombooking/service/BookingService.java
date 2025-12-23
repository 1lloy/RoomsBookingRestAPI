package com.illoy.roombooking.service;

import com.illoy.roombooking.database.entity.*;
import com.illoy.roombooking.database.repository.BookingRepository;
import com.illoy.roombooking.database.repository.RoomRepository;
import com.illoy.roombooking.database.repository.UserRepository;
import com.illoy.roombooking.dto.request.BookingCreateRequest;
import com.illoy.roombooking.dto.response.BookingResponse;
import com.illoy.roombooking.exception.*;
import com.illoy.roombooking.mapper.*;
import com.illoy.roombooking.security.AuthenticationService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    private final AuthenticationService authenticationService;

    private final UserMapper userMapper;
    private final RoomMapper roomMapper;
    private final BookingMapper bookingMapper;

    @Transactional
    public BookingResponse create(BookingCreateRequest request) {
        Room room = roomRepository
                .findByIdAndIsActiveTrue(request.getRoomId())
                .orElseThrow(
                        () -> new RoomNotFoundException("Room not found or inactive with id: " + request.getRoomId()));

        User currentUser = authenticationService.getCurrentUser();

        if (!isRoomAvailable(room.getId(), request.getStartTime(), request.getEndTime())) {
            throw new RoomNotAvailableException("Room is not available for selected time");
        }

        validateBookingTime(request.getStartTime(), request.getEndTime());

        Booking booking = bookingMapper.toEntity(request);
        booking.setRoom(room);
        booking.setUser(currentUser);
        booking.setStatus(BookingStatus.CONFIRMED);

        Booking savedBooking = bookingRepository.save(booking);
        return bookingMapper.toResponse(savedBooking);
    }

    @Transactional
    public BookingResponse cancel(Long bookingId) {
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        User currentUser = authenticationService.getCurrentUser();

        if (!booking.getUser().getUsername().equals(currentUser.getUsername())
                && !currentUser.getRole().equals(UserRole.ROLE_ADMIN)) {
            throw new AccessDeniedException("You can only cancel your own bookings");
        }

        if (booking.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BookingTimeException("Cannot cancel past booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingStatusConflictException("Booking already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking updatedBooking = bookingRepository.saveAndFlush(booking);
        return bookingMapper.toResponse(updatedBooking);
    }

    private boolean isRoomAvailable(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Booking> conflicts = bookingRepository.findConflictingBookings(roomId, startTime, endTime);
        return conflicts.isEmpty();
    }

    private void validateBookingTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            throw new BookingTimeException("End time must be after start time");
        }

        if (startTime.isBefore(LocalDateTime.now())) {
            throw new BookingTimeException("Cannot book in the past");
        }

        // Минимальная продолжительность брони - 30 минут
        if (Duration.between(startTime, endTime).toMinutes() < 30) {
            throw new BookingTimeException("Minimum booking duration is 30 minutes");
        }
    }

    @Transactional
    public BookingResponse updateStatus(Long id, BookingStatus status) {
        Booking booking = bookingRepository
                .findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        if (booking.getStatus() == status) {
            throw new BookingStatusConflictException("Booking already has status: " + status);
        }

        booking.setStatus(status);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    // методы поиска
    public List<BookingResponse> findAll() {
        return bookingRepository.findAll().stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    public BookingResponse findById(Long id) {

        User currentUser = authenticationService.getCurrentUser();

        Booking booking =
                bookingRepository.findById(id).orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        if (!booking.getUser().getUsername().equals(currentUser.getUsername())
                && !currentUser.getRole().equals(UserRole.ROLE_ADMIN)) {
            throw new AccessDeniedException("You can only check your own bookings");
        } else {
            return bookingMapper.toResponse(booking);
        }
    }

    public Page<BookingResponse> findUserBookings(
            Pageable pageable, BookingStatus status, LocalDate fromDate, LocalDate toDate) {

        User currentUser = authenticationService.getCurrentUser();

        if (status != null) {
            return bookingRepository
                    .findByUserIdAndStatus(currentUser.getId(), status, pageable)
                    .map(bookingMapper::toResponse);
        }

        // Если указаны даты - фильтруем по дате
        if (fromDate != null || toDate != null) {
            LocalDateTime start = fromDate != null
                    ? fromDate.atStartOfDay()
                    : LocalDateTime.now().minusYears(100);
            LocalDateTime end = toDate != null
                    ? toDate.atTime(LocalTime.MAX)
                    : LocalDateTime.now().plusYears(100);

            return bookingRepository
                    .findByUserIdAndStartTimeBetween(currentUser.getId(), start, end, pageable)
                    .map(bookingMapper::toResponse);
        }

        return bookingRepository
                .findByUserIdOrderByStartTimeDesc(currentUser.getId(), pageable)
                .map(bookingMapper::toResponse);
    }

    public Page<BookingResponse> findByStatus(BookingStatus status, Pageable pageable) {
        return bookingRepository.findByStatus(status, pageable).map(bookingMapper::toResponse);
    }

    public Map<String, Long> findCountByPeriodGroupByStatus(LocalDateTime start, LocalDateTime end) {

        Map<String, Long> resultMap = new HashMap<>();

        List<Object[]> queryResult = bookingRepository.getCountGroupByStatus(start, end);

        queryResult.forEach(object -> resultMap.put(String.valueOf(object[0]), (Long) object[1]));

        return resultMap;
    }

    public long countByStartTimeBetween(LocalDateTime start, LocalDateTime end) {
        return bookingRepository.countByStartTimeBetween(start, end);
    }

    public Map<String, Long> findBookingsCountByDow(LocalDateTime start, LocalDateTime end) {

        Map<String, Long> dayStats = new LinkedHashMap<>();
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

        List<Object[]> results = bookingRepository.findBookingsByDayOfWeek(start, end);

        results.forEach(
                object -> dayStats.put(days[((Number) object[0]).intValue()], ((Number) object[1]).longValue()));

        return dayStats;
    }

    public Map<String, Long> findPopularRooms(LocalDateTime start, LocalDateTime end, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> results = bookingRepository.findPopularRooms(start, end, pageable);

        return results.stream()
                .collect(Collectors.toMap(
                        result -> (String) result[0],
                        result -> (Long) result[1],
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
    }

    public Map<String, Long> findUsersBookingsCount(LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = bookingRepository.findUsersBookingsCount(start, end);

        return results.stream()
                .collect(Collectors.toMap(
                        result -> (String) result[0], // user name
                        result -> (Long) result[1], // booking count
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
    }

    public Page<BookingResponse> findByUserId(Long userId, Pageable pageable) {

        if (!userRepository.existsById(userId)) {
            throw new UsernameNotFoundException("User not found with id: " + userId);
        }

        return bookingRepository
                .findByUserIdOrderByStartTimeDesc(userId, pageable)
                .map(bookingMapper::toResponse);
    }
}

package com.illoy.roombooking.service;

import com.illoy.roombooking.database.entity.*;
import com.illoy.roombooking.database.repository.BookingRepository;
import com.illoy.roombooking.database.repository.RoomRepository;
import com.illoy.roombooking.dto.request.BookingCreateRequest;
import com.illoy.roombooking.dto.response.BookingResponse;
import com.illoy.roombooking.dto.response.RoomResponse;
import com.illoy.roombooking.dto.response.UserResponse;
import com.illoy.roombooking.exception.*;
import com.illoy.roombooking.mapper.*;
import com.illoy.roombooking.security.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final AuthenticationService authenticationService;

    private final UserMapper userMapper;
    private final RoomMapper roomMapper;
    private final BookingMapper bookingMapper;

    @Transactional
    public BookingResponse create(BookingCreateRequest request) {
        Room room = roomRepository.findByIdAndIsActiveTrue(request.getRoomId())
                .orElseThrow(() -> new RoomNotFoundException("Room not found or inactive"));

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
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        User currentUser = authenticationService.getCurrentUser();

        if (!booking.getUser().getId().equals(currentUser.getId()) &&
                !currentUser.getRole().equals(UserRole.ROLE_ADMIN)) {
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
    public BookingResponse updateStatus(Long id, BookingStatus status){
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

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

        Booking booking = bookingRepository.findById(id).orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        if (!booking.getUser().getId().equals(currentUser.getId()) &&
                !currentUser.getRole().equals(UserRole.ROLE_ADMIN)) {
            throw new AccessDeniedException("You can only cancel your own bookings");
        }
        else{
            return bookingMapper.toResponse(booking);
        }
    }

    public Page<BookingResponse> findUserBookings(Pageable pageable,
                                                  BookingStatus status,
                                                  LocalDate fromDate,
                                                  LocalDate toDate) {

        User currentUser = authenticationService.getCurrentUser();

        if (status != null) {
            return bookingRepository.findByUserIdAndStatus(currentUser.getId(), status, pageable)
                    .map(bookingMapper::toResponse);
        }

        // Если указаны даты - фильтруем по дате
        if (fromDate != null || toDate != null) {
            LocalDateTime start = fromDate != null ? fromDate.atStartOfDay() : LocalDateTime.MIN;
            LocalDateTime end = toDate != null ? toDate.atTime(LocalTime.MAX) : LocalDateTime.MAX;

            return bookingRepository.findByUserIdAndStartTimeBetween(currentUser.getId(), start, end, pageable)
                    .map(bookingMapper::toResponse);
        }

        return bookingRepository.findByUserIdOrderByStartTimeDesc(currentUser.getId(), pageable)
                .map(bookingMapper::toResponse);
    }


    public Page<BookingResponse> findByStatus(BookingStatus status, Pageable pageable){
        return bookingRepository.findByStatus(status, pageable)
                .map(bookingMapper::toResponse);
    }

    public Map<String, Long> findCountByPeriodGroupByStatus(LocalDateTime start,
                                                            LocalDateTime end){

        Map<String, Long> resultMap = new HashMap<>();

        List<Object[]> queryResult = bookingRepository.getCountGroupByStatus(start, end);

        queryResult.forEach(object -> resultMap.put((String) object[0], (Long) object[1]));

        return resultMap;
    }

    public List<BookingResponse> findByStatusAndStartTimeBetween(BookingStatus status,
                                                                 LocalDateTime start,
                                                                 LocalDateTime end){

        return bookingRepository.findByStatusAndStartTimeBetween(status, start, end).stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Page<BookingResponse> findByStatusAndStartTimeBetween(BookingStatus status,
                                                                 LocalDateTime start,
                                                                 LocalDateTime end,
                                                                 Pageable pageable){

        return bookingRepository.findByStatusAndStartTimeBetween(status, start, end, pageable)
                .map(bookingMapper::toResponse);
    }

    public List<BookingResponse> findByRoomIdAndStartTimeBetweenAndStatusIn(Long roomId,
                                                                            LocalDateTime start,
                                                                            LocalDateTime end,
                                                                            List<BookingStatus> statuses){

        return bookingRepository.findByRoomIdAndStartTimeBetweenAndStatusIn(roomId, start, end, statuses).stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> findByStartTimeBetween(LocalDateTime start, LocalDateTime end){
        return bookingRepository.findByStartTimeBetween(start, end).stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Page<BookingResponse> findByStartTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable){
        return bookingRepository.findByStartTimeBetween(start, end, pageable)
                .map(bookingMapper::toResponse);
    }

    public long countByStartTimeBetween(LocalDateTime start, LocalDateTime end){
        return bookingRepository.countByStartTimeBetween(start, end);
    }

    public long countByStatusAndStartTimeBetween(BookingStatus status, LocalDateTime start, LocalDateTime end){
        return bookingRepository.countByStatusAndStartTimeBetween(status, start, end);
    }

    public long countByUserIdAndStatus(Long userId, BookingStatus status){
        return bookingRepository.countByUserIdAndStatus(userId, status);
    }

    public List<BookingResponse> findUpcomingBookings() {
        LocalDateTime now = LocalDateTime.now();
        return bookingRepository.findUpcomingBookings(now).stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> findExpiredBookings(){
        LocalDateTime now = LocalDateTime.now();
        return bookingRepository.findExpiredBookings(now).stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Map<String, Long> getBookingsCountByRoom(LocalDateTime start, LocalDateTime end) {
        List<Map<String, Object>> results = bookingRepository.getBookingsCountByRoom(start, end);

        return results.stream()
                .collect(Collectors.toMap(
                        result -> (String) result.get("roomName"),  // roomName
                        result -> (Long) result.get("bookingCount"),    // bookingCount
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    public Map<String, Long> findBookingsCountByDow(LocalDateTime start, LocalDateTime end){

        Map<String, Long> dayStats = new LinkedHashMap<>();
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

        List<Object[]> results = bookingRepository.findBookingsByDayOfWeek(start, end);

        results.forEach(object -> dayStats.put(days[(int) object[0]] , (Long) object[1]));

        return dayStats;
    }

    public Double getAverageBookingDuration(LocalDateTime start, LocalDateTime end){
        Double result = bookingRepository.getAverageBookingDuration(start, end);
        return result != null ? Math.round(result * 100.0) / 100.0 : 0.0;
    }

    public Map<RoomResponse, Long> findPopularRooms(LocalDateTime start, LocalDateTime end, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> results = bookingRepository.findPopularRooms(start, end, pageable);

        return results.stream()
                .collect(Collectors.toMap(
                        result -> roomMapper.toResponse((Room) result[0]),
                        result -> (Long) result[1],
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    public Map<UserResponse, Long> findUsersBookingsCount(LocalDateTime start, LocalDateTime end){
        List<Object[]> results = bookingRepository.findActiveUsers(start, end);

        return results.stream()
                .collect(Collectors.toMap(
                        result -> userMapper.toResponse((User) result[0]), //user entity
                        result -> (Long) result[1], //booking count
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    public Page<BookingResponse> findByUserId(Long userId, Pageable pageable) {
        return bookingRepository.findByUserIdOrderByStartTimeDesc(userId, pageable)
                .map(bookingMapper::toResponse);
    }

}

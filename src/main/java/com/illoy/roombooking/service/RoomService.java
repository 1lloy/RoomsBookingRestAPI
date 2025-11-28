package com.illoy.roombooking.service;

import com.illoy.roombooking.database.entity.Booking;
import com.illoy.roombooking.database.entity.BookingStatus;
import com.illoy.roombooking.database.entity.Room;
import com.illoy.roombooking.database.repository.BookingRepository;
import com.illoy.roombooking.database.repository.RoomRepository;
import com.illoy.roombooking.dto.request.RoomCreateEditRequest;
import com.illoy.roombooking.dto.response.RoomAvailabilityResponse;
import com.illoy.roombooking.dto.response.RoomResponse;
import com.illoy.roombooking.dto.response.TimeSlot;
import com.illoy.roombooking.exception.*;
import com.illoy.roombooking.mapper.RoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final RoomMapper roomMapper;

    public List<RoomResponse> findAllActive() {
        return roomRepository.findByIsActiveTrue().stream()
                .map(roomMapper::toResponse)
                .collect(Collectors.toList());
    }

    public Page<RoomResponse> findAllActive(Pageable pageable) {
        return roomRepository.findByIsActiveTrue(pageable)
                .map(roomMapper::toResponse);
    }

    public List<RoomResponse> findAll() {
        return roomRepository.findAll().stream()
                .map(roomMapper::toResponse)
                .collect(Collectors.toList());
    }

    public RoomResponse findActiveRoomById(Long id) {
        return roomRepository.findByIdAndIsActiveTrue(id)
                .map(roomMapper::toResponse)
                .orElseThrow(() -> new RoomNotFoundException("Room not found or inactive with id: " + id));
    }

    public RoomResponse findById(Long id) {
        return roomRepository.findById(id)
                .map(roomMapper::toResponse)
                .orElseThrow(() -> new RoomNotFoundException("Room not found or inactive with id: " + id));
    }

    public List<RoomResponse> findActiveByCapacity(Integer minCapacity) {
        return roomRepository.findActiveRoomsByCapacity(minCapacity).stream()
                .map(roomMapper::toResponse)
                .collect(Collectors.toList());
    }

    public long countActiveRooms() {
        return roomRepository.countActiveRooms();
    }

    public List<RoomResponse> searchActiveByName(String searchTerm) {
        return roomRepository.searchActiveRoomsByName(searchTerm).stream()
                .map(roomMapper::toResponse)
                .collect(Collectors.toList());
    }

    public RoomAvailabilityResponse checkAvailability(Long id, LocalDateTime startTime, LocalDateTime endTime) {
        roomRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new RoomNotFoundException("Room not found or inactive with id: " + id));

        // Проверяем доступность на конкретный интервал
        boolean isAvailable = isRoomAvailable(id, startTime, endTime);

        // Получаем все бронирования на этот день
        LocalDate targetDate = startTime.toLocalDate();
        List<Booking> dayBookings = getBookingsForDate(id, targetDate);

        // Формируем расписание
        List<TimeSlot> busySlots = mapToTimeSlots(dayBookings);

        return RoomAvailabilityResponse.builder()
                .availableForRequestedTime(isAvailable)
                .date(targetDate)
                .busySlots(busySlots)
                .build();
    }

    private boolean isRoomAvailable(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Booking> conflicts = bookingRepository.findConflictingBookings(roomId, startTime, endTime);
        return conflicts.isEmpty();
    }

    private List<Booking> getBookingsForDate(Long roomId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return bookingRepository.findByRoomIdAndStartTimeBetweenAndStatusIn(
                roomId,
                startOfDay,
                endOfDay,
                List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING)
        );
    }

    private List<TimeSlot> mapToTimeSlots(List<Booking> bookings) {
        return bookings.stream()
                .map(booking -> new TimeSlot(
                        booking.getStartTime().toLocalTime(),
                        booking.getEndTime().toLocalTime(),
                        booking.getStatus()
                ))
                .sorted(Comparator.comparing(TimeSlot::getStartTime))
                .collect(Collectors.toList());
    }

    @Transactional
    public RoomResponse create(RoomCreateEditRequest request) {
        if (roomRepository.existsByNameAndIsActiveTrue(request.getName())) {
            throw new RoomAlreadyExistsException("Room with name already exists: " + request.getName());
        }

        return Optional.of(request)
                .map(roomMapper::toEntity)
                .map(room -> {
                    room.setActive(true);
                    return room;
                })
                .map(roomRepository::save)
                .map(roomMapper::toResponse)
                .orElseThrow(() -> new RoomCreationException("Failed to create room"));
    }

    @Transactional
    public RoomResponse update(Long id, RoomCreateEditRequest request) {

        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException("Room not found or inactive with id: " + id));

        if (!room.getName().equals(request.getName()) &&
                roomRepository.existsByNameAndIsActiveTrue(request.getName())) {
            throw new RoomAlreadyExistsException("Room with name already exists: " + request.getName());
        }

        roomMapper.updateEntity(request, room);

        return roomMapper.toResponse(roomRepository.saveAndFlush(room));
    }

    @Transactional
    public void updateRoomStatus(Long roomId, boolean active) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found or inactive with id: " + roomId));

        if (room.isActive() == active) {
            throw new RoomStatusConflictException(
                    String.format("Room already has status: %s", active ? "active" : "inactive")
            );
        }

        if (!active){
            if (!hasActiveBookings(roomId)) roomRepository.updateRoomStatus(roomId, false);
            else throw new RoomHasActiveBookingsException(
                    "Cannot delete room with active bookings. Cancel bookings first."
            );
        }
        else roomRepository.updateRoomStatus(roomId, true);
    }

    private boolean hasActiveBookings(Long roomId) {
        // Ищем активные бронирования (текущие и будущие)
        List<Booking> activeBookings = bookingRepository.findConflictingBookings(
                roomId,
                LocalDateTime.now(),
                LocalDateTime.now().plusYears(100)
        );

        return !activeBookings.isEmpty();
    }
}

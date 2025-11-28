package com.illoy.roombooking.controller.admin;

import com.illoy.roombooking.database.entity.BookingStatus;
import com.illoy.roombooking.dto.request.BookingStatusUpdateRequest;
import com.illoy.roombooking.dto.response.BookingResponse;
import com.illoy.roombooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/bookings")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminBookingController {

    private final BookingService bookingService;

    //получить бронирования по статусу (с пагинацией)
    @GetMapping
    public ResponseEntity<?> findAllByStatus(@RequestParam(required = false) BookingStatus status,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(defaultValue = "startTime") String sortBy){

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).descending());

        if (status != null) {
            return ResponseEntity.ok(bookingService.findByStatus(status, pageable));
        } else {
            return ResponseEntity.ok(bookingService.findAll());
        }
    }

    //поиск бронирований пользователя по id
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<BookingResponse>> getBookingsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
        Page<BookingResponse> bookings = bookingService.findByUserId(userId, pageable);
        return ResponseEntity.ok(bookings);
    }

    //обновить статус бронирования
    @PatchMapping("/{bookingId}/status")
    public ResponseEntity<BookingResponse> updateBookingStatus(@PathVariable("bookingId") Long bookingId,
                                                               @RequestBody @Valid BookingStatusUpdateRequest request) {

        BookingResponse updatedBooking = bookingService.updateStatus(bookingId, request.getStatus());
        return ResponseEntity.ok(updatedBooking);
    }
}

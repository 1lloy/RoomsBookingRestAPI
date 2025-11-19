package com.illoy.roombooking.controller;

import com.illoy.roombooking.database.entity.Booking;
import com.illoy.roombooking.dto.request.BookingCreateRequest;
import com.illoy.roombooking.dto.response.BookingResponse;
import com.illoy.roombooking.security.AuthenticationService;
import com.illoy.roombooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;
    private final AuthenticationService authenticationService;

    //создать бронирование
    @PostMapping
    public ResponseEntity<BookingResponse> create(@RequestBody @Valid BookingCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(request));
    }

    //отменить бронирование
    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingResponse> cancel(@PathVariable("bookingId") Long id){
        return ResponseEntity.ok(bookingService.cancel(id));
    }

    //получить конкретное бронирование
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> findById(@PathVariable("bookingId") Long id){

        BookingResponse bookingResponse = bookingService.findById(id);

        return ResponseEntity.ok(bookingResponse);
    }
}

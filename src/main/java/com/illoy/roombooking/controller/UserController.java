package com.illoy.roombooking.controller;

import com.illoy.roombooking.database.entity.BookingStatus;
import com.illoy.roombooking.dto.request.UserEditRequest;
import com.illoy.roombooking.dto.response.BookingResponse;
import com.illoy.roombooking.dto.response.UserResponse;
import com.illoy.roombooking.exception.UserUpdateException;
import com.illoy.roombooking.security.AuthenticationService;
import com.illoy.roombooking.service.BookingService;
import com.illoy.roombooking.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final BookingService bookingService;
    private final AuthenticationService authenticationService;

    //получение текущего пользователя
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        UserResponse userResponse = userService.findById(authenticationService.getCurrentUserId())
                .orElseThrow(
                        () -> new UsernameNotFoundException("User with id = " + authenticationService.getCurrentUserId() + " not found")
                );

        return ResponseEntity.ok(userResponse);
    }

    //обновление пользователя
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(@Valid @RequestBody UserEditRequest editRequest) {
        UserResponse updatedUser = userService.update(authenticationService.getCurrentUserId(), editRequest)
                .orElseThrow(() -> new UserUpdateException("Failed to update user with id: " + authenticationService.getCurrentUserId()));

        return ResponseEntity.ok(updatedUser);
    }

    //получить бронирования пользователя
    @GetMapping("/me/bookings")
    public ResponseEntity<List<BookingResponse>> getUserBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
        Page<BookingResponse> bookings = bookingService.findUserBookings(pageable, status, fromDate, toDate);

        return ResponseEntity.ok(bookings.getContent());
    }
}

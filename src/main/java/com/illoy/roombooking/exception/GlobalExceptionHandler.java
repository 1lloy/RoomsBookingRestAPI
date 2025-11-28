package com.illoy.roombooking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        ErrorResponse error = new ErrorResponse(
                "INVALID_CREDENTIALS",
                "Invalid username or password",
                HttpStatus.UNAUTHORIZED.value(),
                null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledUser(DisabledException ex) {
        ErrorResponse error = new ErrorResponse(
                "USER_DISABLED",
                "User account is disabled",
                HttpStatus.UNAUTHORIZED.value(),
                null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLockedUser(LockedException ex) {
        ErrorResponse error = new ErrorResponse(
                "USER_LOCKED",
                "User account is locked",
                HttpStatus.UNAUTHORIZED.value(),
                null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach((error) -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .error("VALIDATION_FAILED")
                        .message("Request validation failed")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .certainErrors(errors)
                        .build());
    }

    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoomNotFound(RoomNotFoundException ex) {

        ErrorResponse response = ErrorResponse.builder()
                .error("ROOM_NOT_FOUND")
                .message(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(RoomNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleRoomNotAvailable(RoomNotAvailableException ex) {

        ErrorResponse response = ErrorResponse.builder()
                .error("ROOM_NOT_AVAILABLE_FOR_THIS_TIME")
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RoomAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleRoomAlreadyExistsException(RoomAlreadyExistsException ex) {

        ErrorResponse response = ErrorResponse.builder()
                .error("ROOM_ALREADY_EXISTS")
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RoomCreationException.class)
    public ResponseEntity<ErrorResponse> handleRoomCreationException(RoomCreationException ex) {

        ErrorResponse response = ErrorResponse.builder()
                .error("ROOM_CREATE_ERROR")
                .message(ex.getMessage())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(RoomStatusConflictException.class)
    public ResponseEntity<ErrorResponse> handleRoomStatusConflictException(RoomStatusConflictException ex) {

        ErrorResponse response = ErrorResponse.builder()
                .error("ROOM_STATUS_CONFLICT")
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RoomHasActiveBookingsException.class)
    public ResponseEntity<ErrorResponse> handleRoomHasActiveBookingsException(RoomHasActiveBookingsException ex) {

        ErrorResponse response = ErrorResponse.builder()
                .error("ROOM_BOOKINGS_CONFLICT")
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookingNotFound(BookingNotFoundException ex) {

        ErrorResponse response = ErrorResponse.builder()
                .error("BOOKING_NOT_FOUND")
                .message(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(BookingTimeException.class)
    public ResponseEntity<ErrorResponse> handleBookingTimeException(BookingTimeException ex) {

        ErrorResponse response = ErrorResponse.builder()
                .error("BOOKING_TIME_CONFLICT")
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {

        ErrorResponse response = ErrorResponse.builder()
                .error("ACCESS_DENIED")
                .message(ex.getMessage())
                .status(HttpStatus.FORBIDDEN.value())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(BookingStatusConflictException.class)
    public ResponseEntity<ErrorResponse> handleBookingStatusConflictException(BookingStatusConflictException ex) {

        ErrorResponse response = ErrorResponse.builder()
                .error("BOOKING_STATUS_CONFLICT")
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(UsernameNotFoundException ex) {

        ErrorResponse response = ErrorResponse.builder()
                .error("USER_NOT_FOUND")
                .message(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(UsernameStatusConflictException.class)
    public ResponseEntity<ErrorResponse> handleUsernameStatusConflictException(UsernameStatusConflictException ex) {

        ErrorResponse response = ErrorResponse.builder()
                .error("USER_STATUS_CONFLICT")
                .message(ex.getMessage())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}

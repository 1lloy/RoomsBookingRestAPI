package com.illoy.roombooking.exception;

public class BookingStatusConflictException extends RuntimeException {
    public BookingStatusConflictException(String message) {
        super(message);
    }
}

package com.illoy.roombooking.exception;

public class RoomHasActiveBookingsException extends RuntimeException {
    public RoomHasActiveBookingsException(String message) {
        super(message);
    }
}

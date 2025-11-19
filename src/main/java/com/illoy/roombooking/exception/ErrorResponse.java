package com.illoy.roombooking.exception;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ErrorResponse {
    String error;
    String message;
    int status;
    LocalDateTime timestamp = LocalDateTime.now();
}

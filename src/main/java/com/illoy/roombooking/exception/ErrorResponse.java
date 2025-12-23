package com.illoy.roombooking.exception;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ErrorResponse {
    String error;
    String message;
    int status;
    LocalDateTime timestamp = LocalDateTime.now();
    Map<String, String> certainErrors;
}

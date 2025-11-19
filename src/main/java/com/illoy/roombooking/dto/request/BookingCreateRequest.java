package com.illoy.roombooking.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class BookingCreateRequest {

    @NotNull(message = "Room ID is required")
    Long roomId;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    LocalDateTime endTime;
}

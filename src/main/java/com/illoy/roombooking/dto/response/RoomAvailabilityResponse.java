package com.illoy.roombooking.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class RoomAvailabilityResponse {
    boolean availableForRequestedTime;
    LocalDate date;
    List<TimeSlot> busySlots;
}

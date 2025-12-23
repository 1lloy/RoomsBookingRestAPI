package com.illoy.roombooking.dto.response;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RoomAvailabilityResponse {
    boolean availableForRequestedTime;
    LocalDate date;
    List<TimeSlot> busySlots;
}

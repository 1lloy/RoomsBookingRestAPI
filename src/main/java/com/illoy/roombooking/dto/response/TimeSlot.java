package com.illoy.roombooking.dto.response;

import com.illoy.roombooking.database.entity.BookingStatus;
import lombok.Value;

import java.time.LocalTime;

@Value
public class TimeSlot {
    LocalTime startTime;
    LocalTime endTime;
    BookingStatus status;

    public String getFormattedTime() {
        return startTime + " - " + endTime;
    }

    public String getStatusDescription() {
        return status == BookingStatus.CONFIRMED ? "Confirmed" : "Pending";
    }
}

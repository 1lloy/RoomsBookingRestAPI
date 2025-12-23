package com.illoy.roombooking.dto.response;

import com.illoy.roombooking.database.entity.BookingStatus;
import java.time.LocalTime;
import lombok.Value;

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

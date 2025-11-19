package com.illoy.roombooking.dto.request;

import com.illoy.roombooking.database.entity.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

@Value
public class BookingStatusUpdateRequest {
    @NotNull
    BookingStatus status;
}

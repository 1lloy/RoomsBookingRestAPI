package com.illoy.roombooking.dto.response;

import com.illoy.roombooking.database.entity.BookingStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BookingResponse {
    Long id;
    LocalDateTime startTime;
    LocalDateTime endTime;
    BookingStatus status;

    // Room info
    Long roomId;
    String roomName;
    Integer roomCapacity;

    // User info
    Long userId;
    String userName;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

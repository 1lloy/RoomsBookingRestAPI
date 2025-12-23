package com.illoy.roombooking.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class RoomResponse {
    Long id;
    String name;
    String description;
    Integer capacity;
    boolean active;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

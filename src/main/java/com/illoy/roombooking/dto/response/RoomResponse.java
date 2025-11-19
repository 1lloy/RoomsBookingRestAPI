package com.illoy.roombooking.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

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

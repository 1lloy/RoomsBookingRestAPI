package com.illoy.roombooking.dto.request;

import com.illoy.roombooking.group.CreateAction;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RoomCreateEditRequest {
    @NotNull(groups = {CreateAction.class})
    @Size(min = 2, max = 50)
    String name;

    @Size(max = 200)
    String description;

    @NotNull(groups = {CreateAction.class})
    @Min(1)
    @Max(100)
    Integer capacity;
}

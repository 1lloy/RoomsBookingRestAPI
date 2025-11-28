package com.illoy.roombooking.dto.request;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserStatusUpdateRequest {
    boolean active;
}

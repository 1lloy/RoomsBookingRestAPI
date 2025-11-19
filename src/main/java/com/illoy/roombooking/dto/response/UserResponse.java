package com.illoy.roombooking.dto.response;

import com.illoy.roombooking.database.entity.UserRole;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class UserResponse {
    Long id;
    String username;
    String email;
    UserRole role;
    boolean active;
}

package com.illoy.roombooking.dto.response;

import com.illoy.roombooking.database.entity.UserRole;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class JwtResponse {
    String token;
    String type = "Bearer";
    String username;
    UserRole role;
}

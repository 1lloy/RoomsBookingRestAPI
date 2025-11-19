package com.illoy.roombooking.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RegisterResponse {
    String token;
    String type = "Bearer";
    UserResponse user;
}

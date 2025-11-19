package com.illoy.roombooking.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserEditRequest {

    @Email
    @Nullable
    String email;

    @Size(min = 6, max = 40)
    @Nullable
    String password;
}

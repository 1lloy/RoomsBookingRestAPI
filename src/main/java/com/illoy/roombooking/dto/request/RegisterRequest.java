package com.illoy.roombooking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class RegisterRequest {
    @NotBlank
    @Size(min = 3, max = 20)
    String username;

    @NotBlank
    @Email
    String email;

    @NotBlank
    @Size(min = 6, max = 40)
    String password;
}

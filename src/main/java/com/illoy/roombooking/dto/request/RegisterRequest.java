package com.illoy.roombooking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RegisterRequest {
    @NotBlank
    @Size(min = 3, max = 20, message = "Username должен иметь длину от 3 до 20 символов")
    String username;

    @NotBlank
    @Email(message = "Адрес электронный почты не является корректным")
    String email;

    @NotBlank
    @Size(min = 6, max = 40, message = "Пароль должен иметь длину от 6 до 40 символов")
    String password;
}

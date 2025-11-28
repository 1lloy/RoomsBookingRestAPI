package com.illoy.roombooking.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.illoy.roombooking.database.entity.User;
import com.illoy.roombooking.database.entity.UserRole;
import com.illoy.roombooking.database.repository.UserRepository;
import com.illoy.roombooking.dto.request.LoginRequest;
import com.illoy.roombooking.dto.request.RegisterRequest;
import com.illoy.roombooking.dto.response.JwtResponse;
import com.illoy.roombooking.dto.response.RegisterResponse;
import com.illoy.roombooking.exception.ErrorResponse;
import com.illoy.roombooking.integration.IntegrationTestBase;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest extends IntegrationTestBase {

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        User user2 = User.builder()
                .username("john")
                .email("johnHjon@gmail.com")
                .password("{noop}123")
                .role(UserRole.ROLE_USER)
                .isActive(true)
                .build();

        userRepository.save(user2);
    }

    @Test
    void authenticateUser_shouldReturnJwtResponseSuccess() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder().username("john").password("123").build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JwtResponse jwtResponse = objectMapper.readValue(result.getResponse().getContentAsString(), JwtResponse.class);

        assertEquals("john",  jwtResponse.getUsername());
        assertThat(jwtResponse.getToken()).isNotNull();
        assertThat(jwtResponse.getToken()).isNotEmpty();
        assertEquals(UserRole.ROLE_USER, jwtResponse.getRole());
    }

    @Test
    void authenticateUser_shouldReturnErrorResponseWithInvalidCredentials() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder().username("john").password("randomIncorrectPassword").build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().is4xxClientError())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertEquals("INVALID_CREDENTIALS",  errorResponse.getError());
        assertEquals("Invalid username or password",  errorResponse.getMessage());
        assertEquals(401,  errorResponse.getStatus());
    }

    @Test
    void registerUser_shouldReturnRegisterResponseSuccess() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("newUser")
                .email("newUserEmail@gmail.com")
                .password("newUserPassword")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        RegisterResponse registerResponse = objectMapper.readValue(result.getResponse().getContentAsString(), RegisterResponse.class);

        assertEquals("newUser",  registerResponse.getUser().getUsername());
        assertEquals("newUserEmail@gmail.com",  registerResponse.getUser().getEmail());
        assertEquals(UserRole.ROLE_USER, registerResponse.getUser().getRole());
        assertThat(registerResponse.getUser().isActive()).isTrue();
        assertThat(registerResponse.getToken()).isNotNull();
        assertThat(registerResponse.getToken()).isNotEmpty();
    }

    @Test
    void registerUser_shouldReturnErrorResponseWithInvalidCredentials() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("newUserNewUserNewUserNewUser")
                .email("newUserEmail@gmail.com")
                .password("new")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().is4xxClientError())
                .andReturn();

        ErrorResponse errorResponse = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

        assertEquals("VALIDATION_FAILED",  errorResponse.getError());
        assertEquals("Request validation failed",  errorResponse.getMessage());
        assertEquals(400,  errorResponse.getStatus());
        assertEquals("Пароль должен иметь длину от 6 до 40 символов",  errorResponse.getCertainErrors().get("password"));
        assertEquals("Username должен иметь длину от 3 до 20 символов",  errorResponse.getCertainErrors().get("username"));
    }
}

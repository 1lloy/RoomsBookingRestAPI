package com.illoy.roombooking.controller;

import com.illoy.roombooking.database.entity.UserRole;
import com.illoy.roombooking.dto.request.LoginRequest;
import com.illoy.roombooking.dto.request.RegisterRequest;
import com.illoy.roombooking.exception.ErrorResponse;
import com.illoy.roombooking.dto.response.JwtResponse;
import com.illoy.roombooking.dto.response.RegisterResponse;
import com.illoy.roombooking.dto.response.UserResponse;
import com.illoy.roombooking.security.UserPrincipal;
import com.illoy.roombooking.security.jwt.JwtUtils;
import com.illoy.roombooking.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateJwtToken(authentication);

        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();

        return ResponseEntity.ok(JwtResponse.builder()
                .token(jwt)
                .username(userDetails.getUsername())
                .role
                        (
                                userDetails.getAuthorities().stream()
                                        .findFirst()
                                        .map(GrantedAuthority::getAuthority)
                                        .map(UserRole::valueOf)
                                        .orElse(UserRole.ROLE_USER)
                        )
                .build()
        );
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        UserResponse userResponse = userService.create(registerRequest);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(registerRequest.getUsername(), registerRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateJwtToken(authentication);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RegisterResponse.builder()
                        .token(jwt)
                        .user(userResponse)
                        .build());
    }
}

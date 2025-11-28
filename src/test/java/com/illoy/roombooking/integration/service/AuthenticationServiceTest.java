package com.illoy.roombooking.integration.service;

import com.illoy.roombooking.exception.UserNotAuthenticatedException;
import com.illoy.roombooking.integration.IntegrationTestBase;
import com.illoy.roombooking.security.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class AuthenticationServiceTest extends IntegrationTestBase {

    private final AuthenticationService authenticationService;

    @Test
    void getCurrentUser_WhenAuthenticationIsNull_ThrowsUserNotAuthenticatedException() {
        // Given - очищаем SecurityContext
        SecurityContextHolder.clearContext();

        // When & Then
        assertThrows(UserNotAuthenticatedException.class, authenticationService::getCurrentUser);
    }

    @Test
    void getCurrentUser_WhenUserNotFound_ThrowsUsernameNotFoundException() {
        // Given
        String username = "testuser";
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When & Then
        assertThrows(UsernameNotFoundException.class, authenticationService::getCurrentUser);
    }
}

package com.illoy.roombooking.controller.admin;

import com.illoy.roombooking.database.entity.UserRole;
import com.illoy.roombooking.dto.request.UserStatusUpdateRequest;
import com.illoy.roombooking.dto.response.UserResponse;
import com.illoy.roombooking.service.UserService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminUserController {

    private final UserService userService;

    //получить всех пользователей
    @GetMapping("/all")
    public ResponseEntity<List<UserResponse>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    //получить всех активных пользователей (с пагинацией)
    @GetMapping("/all/active")
    public ResponseEntity<Page<UserResponse>> findAllActive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "username") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<UserResponse> activeUsers = userService.findAllActiveUsers(pageable);

        return ResponseEntity.ok(activeUsers);
    }

    //получить пользователей определенной роли (с пагинацией)
    @GetMapping("/by-role")
    public ResponseEntity<Page<UserResponse>> findByRole(
            @RequestParam @NotNull UserRole role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "username") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<UserResponse> users = userService.findByRole(role, pageable);

        return ResponseEntity.ok(users);
    }

    //получить пользователя по email
    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> findByEmail(@RequestBody @NotNull String email) {

        UserResponse user = userService.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return ResponseEntity.ok(user);
    }

    //получить пользователя по username
    @GetMapping("/by-username")
    public ResponseEntity<UserResponse> findByUsername(@RequestBody @NotNull String username) {

        UserResponse user = userService.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return ResponseEntity.ok(user);
    }

    //обновить статус пользователя
    @PatchMapping("/{userId}/status")
    public ResponseEntity<Map<String, Object>> updateUserStatus(@PathVariable Long userId,
                                                                @RequestBody UserStatusUpdateRequest request) {

        boolean isUpdated = userService.updateStatus(userId, request.isActive());

        Map<String, Object> response = new HashMap<>();
        response.put("success", isUpdated);
        response.put("message", isUpdated ?
                "User status updated successfully" :
                "User status was already set to requested value");
        response.put("userId", userId);
        response.put("active", request.isActive());

        return ResponseEntity.ok(response);
    }
}

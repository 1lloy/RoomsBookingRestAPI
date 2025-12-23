package com.illoy.roombooking.service;

import com.illoy.roombooking.database.entity.User;
import com.illoy.roombooking.database.entity.UserRole;
import com.illoy.roombooking.database.repository.UserRepository;
import com.illoy.roombooking.dto.request.RegisterRequest;
import com.illoy.roombooking.dto.request.UserEditRequest;
import com.illoy.roombooking.dto.response.UserResponse;
import com.illoy.roombooking.exception.EmailAlreadyExistsException;
import com.illoy.roombooking.exception.UserCreationException;
import com.illoy.roombooking.exception.UsernameAlreadyExistsException;
import com.illoy.roombooking.exception.UsernameStatusConflictException;
import com.illoy.roombooking.mapper.UserMapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public Optional<UserResponse> findByUsername(String username) {
        return userRepository.findByUsername(username).map(userMapper::toResponse);
    }

    public Optional<UserResponse> findByEmail(String email) {
        return userRepository.findByEmail(email).map(userMapper::toResponse);
    }

    public Page<UserResponse> findByRole(UserRole role, Pageable pageable) {
        return userRepository.findByRole(role, pageable).map(userMapper::toResponse);
    }

    public Page<UserResponse> findAllActiveUsers(Pageable pageable) {
        return userRepository.findAllActiveUsers(pageable).map(userMapper::toResponse);
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(userMapper::toResponse).collect(Collectors.toList());
    }

    public Optional<UserResponse> findById(Long id) {
        return userRepository.findById(id).map(userMapper::toResponse);
    }

    public long countActiveUsers() {
        return userRepository.countActiveUsers();
    }

    @Transactional
    public UserResponse create(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + registerRequest.getEmail());
        }
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UsernameAlreadyExistsException("Username already exists: " + registerRequest.getUsername());
        }

        return Optional.of(registerRequest)
                .map(userMapper::toEntity)
                .map(user -> {
                    if (user.getRole() == null) user.setRole(UserRole.ROLE_USER);
                    user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
                    return user;
                })
                .map(userRepository::save)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new UserCreationException("Failed to create user"));
    }

    @Transactional
    public Optional<UserResponse> update(Long id, UserEditRequest editRequest) {

        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        if (editRequest.getEmail() != null) {
            if (!user.getEmail().equals(editRequest.getEmail())
                    && userRepository.existsByEmail(editRequest.getEmail())) {
                throw new EmailAlreadyExistsException("Email already busy: " + editRequest.getEmail());
            }
            user.setEmail(editRequest.getEmail());
        }

        if (editRequest.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(editRequest.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        return Optional.ofNullable(userMapper.toResponse(updatedUser));
    }

    @Transactional
    public boolean updateStatus(Long userId, boolean active) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Failed to retrieve user with id: " + userId));

        if (user.isActive() != active) {
            userRepository.updateUserStatus(userId, active);
            return true;
        } else {
            throw new UsernameStatusConflictException("Username already has this status");
        }
    }
}

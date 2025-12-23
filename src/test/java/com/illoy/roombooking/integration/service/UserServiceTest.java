package com.illoy.roombooking.integration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.illoy.roombooking.database.entity.User;
import com.illoy.roombooking.database.entity.UserRole;
import com.illoy.roombooking.database.repository.UserRepository;
import com.illoy.roombooking.dto.request.RegisterRequest;
import com.illoy.roombooking.dto.request.UserEditRequest;
import com.illoy.roombooking.dto.response.UserResponse;
import com.illoy.roombooking.exception.EmailAlreadyExistsException;
import com.illoy.roombooking.exception.UsernameAlreadyExistsException;
import com.illoy.roombooking.exception.UsernameStatusConflictException;
import com.illoy.roombooking.integration.IntegrationTestBase;
import com.illoy.roombooking.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class UserServiceTest extends IntegrationTestBase {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private static final String USERNAME = "john";
    private static final String EMAIL = "john@example.com";
    private static Long ID;

    @BeforeEach
    void setUp() {
        User user1 = User.builder()
                .username(USERNAME)
                .email(EMAIL)
                .password("123")
                .role(UserRole.ROLE_USER)
                .isActive(true)
                .build();
        User user2 = User.builder()
                .username("anna")
                .email("anna@gmail.com")
                .password("123")
                .role(UserRole.ROLE_USER)
                .isActive(true)
                .build();
        User user3 = User.builder()
                .username("oleg")
                .email("oleg@gmail.com")
                .password("123")
                .role(UserRole.ROLE_USER)
                .isActive(false)
                .build();
        User admin1 = User.builder()
                .username("mark")
                .email("mark@gmail.com")
                .password("123")
                .role(UserRole.ROLE_ADMIN)
                .isActive(true)
                .build();

        userRepository.saveAll(List.of(user1, user2, user3, admin1));

        ID = user1.getId();
    }

    @Test
    void findByUsername_shouldReturnUserResponse() {
        Optional<UserResponse> result = userService.findByUsername(USERNAME);

        assertTrue(result.isPresent());
        assertEquals(USERNAME, result.get().getUsername());
        assertEquals(EMAIL, result.get().getEmail());
    }

    @Test
    void findByEmail_shouldReturnUserResponse() {
        Optional<UserResponse> result = userService.findByEmail(EMAIL);

        assertTrue(result.isPresent());
        assertEquals(USERNAME, result.get().getUsername());
        assertEquals(EMAIL, result.get().getEmail());
    }

    @Test
    void findByRole_shouldReturnPageResponse() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("username").ascending());

        // when
        Page<UserResponse> result = userService.findByRole(UserRole.ROLE_USER, pageable);

        // then
        assertEquals(3, result.getTotalElements());
        assertThat(result).extracting(UserResponse::getUsername).containsExactly("anna", "john", "oleg");
    }

    @Test
    void findAllActiveUsers_shouldReturnOnlyActive() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("username").ascending());

        // when
        Page<UserResponse> result = userService.findAllActiveUsers(pageable);

        // then
        assertEquals(2, result.getTotalElements());
        assertThat(result).extracting(UserResponse::getUsername).containsExactly("anna", "john");
        assertThat(result).noneMatch(userResponse -> !userResponse.isActive());
        assertThat(result).noneMatch(userResponse -> userResponse.getRole() == UserRole.ROLE_ADMIN);
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        // when
        List<UserResponse> result = userService.findAll();

        // then
        assertThat(result).hasSize(4);

        // проверяем наличие админа в списке
        assertThat(result).anySatisfy(u -> {
            assertThat(u.getUsername()).isEqualTo("mark");
            assertThat(u.getRole()).isEqualTo(UserRole.ROLE_ADMIN);
        });
    }

    @Test
    void findById_shouldReturnUserResponse() {
        // when
        Optional<UserResponse> result = userService.findById(ID);

        // then
        assertTrue(result.isPresent());
        assertEquals("john", result.get().getUsername());
        assertEquals("john@example.com", result.get().getEmail());
    }

    @Test
    void countActiveUsers_shouldReturnCountOnlyActive() {
        // when
        Long count = userService.countActiveUsers();

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void create_shouldReturnUserResponseSuccessfully() {
        // given
        RegisterRequest request = RegisterRequest.builder()
                .username("mike")
                .email("mike@example.com")
                .password("123")
                .build();

        // when
        UserResponse result = userService.create(request);

        // then
        assertEquals(request.getUsername(), result.getUsername());
        assertEquals(request.getEmail(), result.getEmail());
        assertEquals(UserRole.ROLE_USER, result.getRole());
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void create_shouldThrowException_whenEmailAlreadyExists() {
        // given
        RegisterRequest request = RegisterRequest.builder()
                .username("newUser")
                .email(EMAIL)
                .password("123")
                .build();

        // expect
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining(EMAIL);
    }

    @Test
    void create_shouldThrowException_whenUsernameAlreadyExists() {
        // given
        RegisterRequest request = RegisterRequest.builder()
                .username(USERNAME)
                .email("newEmail@gmail.com")
                .password("123")
                .build();

        // expect
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessageContaining(USERNAME);
    }

    @Test
    void update_shouldUpdateEmail() {
        // given
        UserEditRequest request =
                UserEditRequest.builder().email("new@example.com").build();

        // when
        Optional<UserResponse> result = userService.update(ID, request);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("new@example.com");
        assertThat(result.get().getUsername()).isEqualTo(USERNAME);
    }

    @Test
    void update_shouldUpdatePassword() {
        // given
        UserEditRequest request =
                UserEditRequest.builder().password("newPassword").build();

        // when
        Optional<UserResponse> result = userService.update(ID, request);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(USERNAME);
    }

    @Test
    void update_shouldUpdateEmailAndPasswordTogether() {
        // given
        UserEditRequest request = UserEditRequest.builder()
                .email("new@example.com")
                .password("newPassword")
                .build();

        // when
        Optional<UserResponse> result = userService.update(ID, request);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo(USERNAME);
        assertThat(result.get().getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void update_shouldThrowException_whenUserNotFound() {
        // given
        UserEditRequest request =
                UserEditRequest.builder().email("new@example.com").build();

        // expect
        assertThatThrownBy(() -> userService.update(-999L, request))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining(String.valueOf(-999L));
    }

    @Test
    void update_shouldThrowException_whenEmailIsBusy() {
        // given
        UserEditRequest request =
                UserEditRequest.builder().email("anna@gmail.com").build();

        // expect
        assertThatThrownBy(() -> userService.update(ID, request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("anna@gmail.com");
    }

    @Test
    void updateStatus_shouldUpdateFromActiveToInactive() {
        // when
        boolean result = userService.updateStatus(ID, false);

        // then
        assertThat(result).isTrue();

        User updated = userRepository.findById(ID).orElseThrow();
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void updateStatus_shouldThrowConflict_whenStatusAlreadySame() {
        assertThatThrownBy(() -> userService.updateStatus(ID, true))
                .isInstanceOf(UsernameStatusConflictException.class)
                .hasMessageContaining("Username already has this status");
    }

    @Test
    void updateStatus_shouldThrowNotFound_whenUserDoesNotExist() {
        assertThatThrownBy(() -> userService.updateStatus(-999L, true))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining(String.valueOf(-999L));
    }
}

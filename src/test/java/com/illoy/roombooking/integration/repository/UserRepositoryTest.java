package com.illoy.roombooking.integration.repository;

import com.illoy.roombooking.database.entity.UserRole;
import com.illoy.roombooking.database.repository.UserRepository;
import com.illoy.roombooking.integration.IntegrationTestBase;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import com.illoy.roombooking.database.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class UserRepositoryTest extends IntegrationTestBase {

    private final UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByEmail(){
        User newUser = new User();
        newUser.setEmail("test@gmail.com");
        newUser.setUsername("test");
        newUser.setPassword("test123");
        newUser.setRole(UserRole.ROLE_USER);

        userRepository.save(newUser);

        Optional<User> result = userRepository.findByEmail("test@gmail.com");

        assertThat(result)
                .as("User should be found by email")
                .isPresent()
                .get()
                .satisfies(user -> {
                    assertThat(user.getUsername()).isEqualTo(newUser.getUsername());
                    assertThat(user.getEmail()).isEqualTo(newUser.getEmail());
                });
    }

    @Test
    void findByUsername(){
        // positive test
        User newUser = new User();
        newUser.setEmail("test@gmail.com");
        newUser.setUsername("test");
        newUser.setPassword("test123");
        newUser.setRole(UserRole.ROLE_USER);

        userRepository.save(newUser);

        Optional<User> result = userRepository.findByUsername("test");

        assertThat(result)
                .as("User should be found by username")
                .isPresent()
                .get()
                .satisfies(user -> {
                    assertThat(user.getUsername()).isEqualTo(newUser.getUsername());
                    assertThat(user.getEmail()).isEqualTo(newUser.getEmail());
                });

        // negative test

        // given
        // база пуста или в ней нет пользователя с username "nonexistentUser"
        User existing = new User();
        existing.setEmail("john@company.com");
        existing.setUsername("john");
        existing.setPassword("pass");
        existing.setRole(UserRole.ROLE_USER);
        existing.setActive(true);

        userRepository.save(existing);

        // when
        Optional<User> noExistingUser = userRepository.findByUsername("nonexistentUser");

        // then
        assertThat(noExistingUser).isNotPresent(); // AssertJ: Optional.empty()
    }

    @Test
    void existsByEmailAndUsername(){
        User newUser = new User();
        newUser.setEmail("test@gmail.com");
        newUser.setUsername("test");
        newUser.setPassword("test123");
        newUser.setRole(UserRole.ROLE_USER);

        userRepository.save(newUser);

        boolean result = userRepository.existsByEmail("test@gmail.com");
        assertThat(result).isTrue();

        boolean resultUsername = userRepository.existsByUsername("randomUsernameThatDontExists");
        assertThat(resultUsername).isFalse();
    }

    @Test
    void shouldFindUsersByRoleWithPagination(){
        // given
        User admin1 = User.builder().email("admin1@company.com").username("admin1").password("pass").role(UserRole.ROLE_ADMIN).build();
        User admin2 = User.builder().email("admin2@company.com").username("admin2").password("pass").role(UserRole.ROLE_ADMIN).build();
        User user1 = User.builder().email("user1@company.com").username("user1").password("pass").role(UserRole.ROLE_USER).build();

        userRepository.saveAll(List.of(admin1, admin2, user1));

        Pageable pageable = PageRequest.of(0, 2, Sort.by("username").ascending());

        // when
        Page<User> page = userRepository.findByRole(UserRole.ROLE_ADMIN, pageable);

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(1);

        assertThat(page.getContent())
                .hasSize(2)
                .extracting(User::getUsername)
                .containsExactly("admin1", "admin2"); // sorted asc

        assertThat(page.isFirst()).isTrue();
        assertThat(page.isLast()).isTrue();
    }

    @Test
    void shouldReturnOnlyActiveUsersWithRoleUserWithPagination() {
        // given
        User activeUser1 = User.builder().email("u1@company.com").username("user1").password("pass").role(UserRole.ROLE_USER)
                .isActive(true).build();
        User activeUser2 = User.builder().email("u2@company.com").username("user2").password("pass").role(UserRole.ROLE_USER)
                .isActive(true).build();

        User inactiveUser = User.builder().email("u3@company.com").username("user3").password("pass").role(UserRole.ROLE_USER)
                .isActive(false).build();
        User activeAdmin = User.builder().email("admin@company.com").username("admin").password("pass").role(UserRole.ROLE_ADMIN)
                .isActive(true).build();

        userRepository.saveAll(List.of(activeUser1, activeUser2, inactiveUser, activeAdmin));

        Pageable pageable = PageRequest.of(0, 10, Sort.by("username").ascending());

        // when
        Page<User> page = userRepository.findAllActiveUsers(pageable);

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .hasSize(2)
                .extracting(User::getUsername)
                .containsExactly("user1", "user2"); // sorted asc

        assertThat(page.isFirst()).isTrue();
        assertThat(page.isLast()).isTrue();
    }

    @Test
    void shouldUpdateUserStatus() {
        //given
        User user = User.builder().email("john@company.com").username("john").password("pass").role(UserRole.ROLE_USER)
                .isActive(true).build();

        user = userRepository.save(user);

        // when
        userRepository.updateUserStatus(user.getId(), false);

        // then
        User updated = userRepository.findById(user.getId()).orElseThrow();

        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void shouldCountOnlyUsersWithRoleUser() {
        // given
        User user1 = User.builder().email("user1@company.com").username("user1").password("pass").role(UserRole.ROLE_USER)
                .isActive(true).build();
        User user2 = User.builder().email("user2@company.com").username("user2").password("pass").role(UserRole.ROLE_USER)
                .isActive(true).build();
        User admin = User.builder().email("admin@company.com").username("admin1").password("pass").role(UserRole.ROLE_ADMIN)
                .isActive(true).build();

        userRepository.saveAll(List.of(user1, user2, admin));

        // when
        long count = userRepository.countActiveUsers();

        // then
        assertThat(count).isEqualTo(2); // только user1 и user2
    }
}

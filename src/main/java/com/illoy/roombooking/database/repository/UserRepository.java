package com.illoy.roombooking.database.repository;

import com.illoy.roombooking.database.entity.User;
import com.illoy.roombooking.database.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Page<User> findByRole(UserRole role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = 'ROLE_USER' AND u.isActive = true")
    Page<User> findAllActiveUsers(Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.isActive = :active WHERE u.id = :userId")
    void updateUserStatus(@Param("userId") Long userId, @Param("active") boolean active);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'ROLE_USER'")
    long countActiveUsers();
}

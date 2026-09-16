package com.example.shared.domain.repository;

import com.example.shared.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.employee WHERE u.username = :username")
    Optional<User> findByUsernameWithEmployee(@Param("username") String username);

    Optional<User> findByEmployeeId(Long employeeId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByPasswordResetToken(String token);

    @Query("SELECT u FROM User u WHERE u.accountLocked = true")
    List<User> findLockedAccounts();

    @Query("SELECT u FROM User u WHERE u.lastLogin < :date OR u.lastLogin IS NULL")
    List<User> findInactiveUsers(@Param("date") LocalDateTime date);

    @Query("SELECT u FROM User u WHERE u.mustChangePassword = true")
    List<User> findUsersRequiringPasswordChange();

    @Query("SELECT u FROM User u WHERE u.accountExpiresAt < :now AND u.accountExpiresAt IS NOT NULL")
    List<User> findExpiredAccounts(@Param("now") LocalDateTime now);
}
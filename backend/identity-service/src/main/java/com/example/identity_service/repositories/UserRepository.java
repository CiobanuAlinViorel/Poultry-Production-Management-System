package com.example.identity_service.repositories;

import com.example.identity_service.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    // primary login lookup
    Optional<User> findByEmail(String email);

    // uniqueness check when creating a user
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}

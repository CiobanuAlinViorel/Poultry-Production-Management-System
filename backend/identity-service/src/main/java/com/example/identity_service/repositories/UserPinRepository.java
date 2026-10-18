package com.example.identity_service.repositories;

import com.example.identity_service.domain.UserPin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserPinRepository extends JpaRepository<UserPin, UUID> {

    Optional<UserPin> findByUserId(UUID userId);
}

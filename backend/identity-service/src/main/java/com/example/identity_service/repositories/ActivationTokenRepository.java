package com.example.identity_service.repositories;

import com.example.identity_service.domain.ActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface ActivationTokenRepository extends JpaRepository<ActivationToken, String> {

    Optional<ActivationToken> findByTokenHash(String tokenHash);

    void deleteByExpiresAtBefore(Instant now);
}

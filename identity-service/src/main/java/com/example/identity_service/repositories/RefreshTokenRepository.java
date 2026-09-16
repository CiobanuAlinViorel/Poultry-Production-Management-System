package com.example.identity_service.repositories;

import com.example.identity_service.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken,String> {
    // used on every refresh request — find the token being presented
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // theft detection — find all tokens in a family to revoke them all
    List<RefreshToken> findByFamilyId(UUID familyId);

    // logout-all — revoke every active session for a user
    List<RefreshToken> findByUserIdAndRevokedAtIsNull(UUID userId);

    // cleanup job (future) — find expired tokens to purge
    List<RefreshToken> findByExpiresAtBefore(Instant now);
}

package com.example.identity_service.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "refresh_token", schema = "identity")
public class RefreshToken {
   @Id
   @Column(name="token_hash", nullable=false, unique=true)
    private String tokenHash;

   @Column(name = "user_id", nullable=false, updatable=false)
    private UUID userId;

   @Column(name="family_id",  nullable=false, updatable=false)
   private UUID familyId;

    @Column(name="issued_at",  nullable=false, updatable=false)
   private Instant issuedAt;

    @Column(name="expires_at",  nullable=false, updatable=false)
   private Instant expiresAt;

    @Column(name="revoked_at")
   private Instant revokedAt;

    @Column(name = "replaced_by_token_hash")
   private String replacedByTokenHash;

    @Column(name = "device_info")
   private String deviceInfo;
}

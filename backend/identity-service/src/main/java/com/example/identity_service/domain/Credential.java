package com.example.identity_service.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "credentials", schema = "identity")
public class Credential {
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "password_updated_at")
    private Instant passwordUpdatedAt;

    @Column(name = "must_change_password")
    private boolean mustChangePassword;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "password_history", columnDefinition = "jsonb")
    private List<String> passwordHistory;

    @Column(name = "mfa_enabled")
    private boolean mfaEnabled;

    @Column(name = "mfa_secret_enc")
    private String mfaSecretEnc;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mfa_recovery_codes", columnDefinition = "jsonb")
    private List<String> mfaRecoveryCodes;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_at")
    private Instant createdAt;
}

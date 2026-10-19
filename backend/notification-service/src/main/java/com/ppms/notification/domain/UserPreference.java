package com.ppms.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_preference", schema = "notification")
public class UserPreference {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "locale")
    private String locale = "ro";

    @Column(name = "email_enabled")
    private boolean emailEnabled = true;

    @Column(name = "push_enabled")
    private boolean pushEnabled = true;

    @Column(name = "quiet_hours")
    private String quietHours;

    @Column(name = "digest")
    private String digest = "NONE";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "muted_alert_codes", columnDefinition = "jsonb")
    private String mutedAlertCodes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }
}

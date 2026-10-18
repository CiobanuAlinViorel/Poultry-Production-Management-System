package com.ppms.notification.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_preference", schema = "notification")
public class UserPreference extends BaseEntity {

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
}

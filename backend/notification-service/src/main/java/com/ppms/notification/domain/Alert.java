package com.ppms.notification.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "alert", schema = "notification")
public class Alert extends BaseEntity {

    @Column(name = "alert_code", nullable = false)
    private String alertCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Column(name = "scope_type")
    private String scopeType;

    @Column(name = "scope_id")
    private UUID scopeId;

    @Column(name = "site_name")
    private String siteName;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params", columnDefinition = "jsonb")
    private String params;

    @Column(name = "title_key")
    private String titleKey;

    @Column(name = "message_key")
    private String messageKey;

    @Column(name = "dedup_key")
    private String dedupKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.OPEN;

    @Column(name = "raised_at", nullable = false)
    private Instant raisedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "occurrences")
    private int occurrences = 1;

    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "ack_reason")
    private String ackReason;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    public enum Severity { INFO, WARNING, CRITICAL }
    public enum Status { OPEN, ACKNOWLEDGED, RESOLVED, EXPIRED }
}

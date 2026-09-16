package com.example.identity_service.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@Table(name = "login_audit_event", schema = "identity")
public class LoginAuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name= "email", nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean success;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_event_reason")
    private LoginEventReason loginEventReason;

    @Column(nullable = false, name = "ip_address")
    private String ipAddress;

    @Column(nullable = false, name = "occurred_at")
    private Instant occurredAt;

    public LoginAuditEvent(
            UUID userId,
            String email,
            boolean success,
            LoginEventReason loginEventReason,
            String ipAddress,
            Instant occurredAt
    ){
        this.userId = userId;
        this.email = email;
        this.success = success;
        this.loginEventReason = loginEventReason;
        this.ipAddress = ipAddress;
        this.occurredAt = occurredAt;
    }
}

package com.example.shared.domain.entity;

import com.example.shared.domain.exception.SessionException;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class Session extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "is_active")
    private Boolean isActive;


    // Domain Methods
    public static Session createNew(User user, String deviceInfo, String ipAddress) {
        return Session.builder()
                .user(user)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .lastAccessedAt(LocalDateTime.now())
                .isActive(true)
                .build();
    }

    public void refresh() {
        if (!isActive) {
            throw new SessionException("Cannot refresh inactive session");
        }
        this.lastAccessedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusHours(24);
    }

    public void invalidate() {
        this.isActive = false;
        this.expiresAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(isActive) && expiresAt.isAfter(LocalDateTime.now());
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
}
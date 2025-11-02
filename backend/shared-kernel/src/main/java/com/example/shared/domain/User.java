package com.example.shared.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    // ✅ ONE-TO-ONE cu Employee (acum funcționează!)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", unique = true)
    private Employee employee; // Poate fi null pentru system users

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked", nullable = false)
    @Builder.Default
    private Boolean accountLocked = false;

    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private Boolean mustChangePassword = false;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "account_expires_at")
    private LocalDateTime accountExpiresAt;

    // ✅ Business logic pentru autentificare
    public void recordSuccessfulLogin() {
        this.lastLogin = LocalDateTime.now();
        this.failedLoginAttempts = 0;

        if (this.accountLocked) {
            this.accountLocked = false;
        }
    }

    public void recordFailedLogin(int maxAttempts) {
        this.failedLoginAttempts++;

        if (this.failedLoginAttempts >= maxAttempts) {
            this.accountLocked = true;
        }
    }

    public void resetPassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.passwordChangedAt = LocalDateTime.now();
        this.mustChangePassword = false;
        this.failedLoginAttempts = 0;
    }

    public void lockAccount() {
        this.accountLocked = true;
    }

    public void unlockAccount() {
        this.accountLocked = false;
        this.failedLoginAttempts = 0;
    }

    public void forcePasswordChange() {
        this.mustChangePassword = true;
    }

    // ✅ Validări
    @Transient
    public boolean isAccountExpired() {
        return accountExpiresAt != null && LocalDateTime.now().isAfter(accountExpiresAt);
    }

    @Transient
    public boolean isPasswordExpired(int passwordExpiryDays) {
        if (passwordChangedAt == null) {
            return false;
        }
        LocalDateTime expiryDate = passwordChangedAt.plusDays(passwordExpiryDays);
        return LocalDateTime.now().isAfter(expiryDate);
    }

    @Transient
    public boolean canLogin() {
        return isActive &&
                !accountLocked &&
                !isAccountExpired() &&
                !mustChangePassword;
    }

    // ✅ Get role from associated Employee
    @Transient
    public com.example.shared.enums.Role getRole() {
        return employee != null ? employee.getRole() : null;
    }

    @Transient
    public String getFullName() {
        return employee != null
                ? employee.getFirstName() + " " + employee.getLastName()
                : username;
    }

    @Transient
    public boolean isSystemUser() {
        return employee == null;
    }

    @Transient
    public boolean hasEmployeeProfile() {
        return employee != null;
    }
}
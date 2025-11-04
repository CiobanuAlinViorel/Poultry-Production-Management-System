package com.example.shared.domain.entity;

import com.example.shared.domain.valueobject.Password;
import com.example.shared.domain.exception.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;


import javax.security.auth.login.AccountLockedException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class User extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "account_locked")
    private Boolean accountLocked;

    @Column(name = "lock_reason")
    private String lockReason;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts;

    @Column(name = "last_login_attempt")
    private LocalDateTime lastLoginAttempt;

    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;

    @Column(name = "must_change_password")
    private Boolean mustChangePassword;

    @Column(name = "password_change_reason")
    private String passwordChangeReason;

    @Column(name = "password_last_changed")
    private LocalDate passwordLastChanged;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @ElementCollection
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    // Domain Methods
    public void attemptAuthentication(Password password, PasswordEncoder passwordEncoder) throws AccountLockedException {
        validateAccountStatus();

        if (!password.matches(this.passwordHash, passwordEncoder)) {
            recordFailedLoginAttempt();
            throw new InvalidCredentialsException("Invalid password");
        }

        recordSuccessfulLogin();
    }

    public void enableAccount(){
        setAccountLocked(false);
    }

    public void disableAccount(String reason){
        setLockReason(reason);
        setAccountLocked(true);}

    public void changePassword(Password newPassword, PasswordEncoder passwordEncoder) {
        validateNewPasswordStrength(newPassword);
        this.passwordHash = newPassword.getHash(passwordEncoder);
        this.passwordLastChanged = LocalDate.now();
        this.mustChangePassword = false;
        resetFailedLoginAttempts();
    }

    public void lockAccount(String reason) {
        this.accountLocked = true;
        this.lockReason = reason;
    }

    public void unlockAccount() {
        this.accountLocked = false;
        this.lockReason = null;
        resetFailedLoginAttempts();
    }

    public void recordFailedLoginAttempt() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null) ? 1 : this.failedLoginAttempts + 1;
        this.lastLoginAttempt = LocalDateTime.now();

        if (this.failedLoginAttempts >= 5) {
            lockAccount("Maximum login attempts exceeded");
        }
    }

    public void recordSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lastLoginDate = LocalDateTime.now();
        this.accountLocked = false;
    }

    private void validateAccountStatus() throws AccountLockedException {
        if (Boolean.TRUE.equals(accountLocked)) {
            throw new AccountLockedException("Account is locked: " + lockReason);
        }
        if (!Boolean.TRUE.equals(isActive)) {
            throw new AccountDisabledException("Account is disabled");
        }
    }

    public void validateNewPasswordStrength(Password newPassword) {
        if (!newPassword.meetsStrengthRequirements()) {
            throw new InvalidPasswordException("Password does not meet strength requirements");
        }
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lastLoginAttempt = null;
    }

    public boolean isPasswordExpired() {
        return passwordLastChanged != null &&
                passwordLastChanged.plusDays(90).isBefore(LocalDate.now());
    }

    public boolean hasExpiredPassword() {
        return isPasswordExpired();
    }

    public boolean isPasswordChangeRequired() {
        return Boolean.TRUE.equals(mustChangePassword);
    }

    public boolean isAccountLocked() {
        return Boolean.TRUE.equals(accountLocked);
    }

    public boolean isAccountActive() {
        return Boolean.TRUE.equals(isActive);
    }

    public boolean hasExceededMaxLoginAttempts() {
        return failedLoginAttempts >= 5;
    }


    public void initiatePasswordReset() {
    }

    public boolean isAccountExpired() {
        return  Boolean.TRUE.equals(accountLocked);
    }
}
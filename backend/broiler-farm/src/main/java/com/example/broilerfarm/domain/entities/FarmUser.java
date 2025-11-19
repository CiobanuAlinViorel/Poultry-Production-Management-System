package com.example.broilerfarm.domain.entities;

import com.example.shared.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "farm_user")
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmUser extends User {

    @OneToOne
    @JoinColumn(name = "id_employee")
    private FarmEmployee employee;

    public FarmUser(Long id, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime resetTokenExpiry, String passwordResetToken, LocalDate passwordLastChanged, String passwordChangeReason, Boolean mustChangePassword, LocalDateTime lastLoginDate, LocalDateTime lastLoginAttempt, Integer failedLoginAttempts, String lockReason, Boolean accountLocked, Boolean isActive, String passwordHash, String email, String username, FarmEmployee employee) {
        super(id, createdAt, updatedAt, resetTokenExpiry, passwordResetToken, passwordLastChanged, passwordChangeReason, mustChangePassword, lastLoginDate, lastLoginAttempt, failedLoginAttempts, lockReason, accountLocked, isActive, passwordHash, email, username);
        this.employee = employee;
    }
}


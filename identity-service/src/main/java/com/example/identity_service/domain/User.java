package com.example.identity_service.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "users", schema = "identity")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Email(message = "The email is not valid")
    private String email;

    private String username;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts;

    @Column(name="last_login_at")
    private Instant lastLoginAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_at")
    private Instant createdAt;


}

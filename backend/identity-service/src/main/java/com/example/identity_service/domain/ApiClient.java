package com.example.identity_service.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "api_client", schema = "identity")
public class ApiClient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "client_id", nullable = false, unique = true)
    private String clientId;

    @Column(name = "secret_hash", nullable = false)
    private String secretHash;

    @Column(name = "name")
    private String name;

    @Column(name = "allowed_scopes")
    private String allowedScopes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ClientStatus status = ClientStatus.ACTIVE;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    public enum ClientStatus {
        ACTIVE, DISABLED
    }
}

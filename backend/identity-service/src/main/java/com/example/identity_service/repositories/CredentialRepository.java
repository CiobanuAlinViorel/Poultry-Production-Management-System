package com.example.identity_service.repositories;

import com.example.identity_service.domain.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CredentialRepository extends JpaRepository<Credential, UUID> {

    // the only query you ever need on credentials
    Optional<Credential> findByUserId(UUID userId);
}

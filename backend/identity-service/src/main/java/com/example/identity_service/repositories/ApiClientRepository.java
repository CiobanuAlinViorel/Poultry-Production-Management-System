package com.example.identity_service.repositories;

import com.example.identity_service.domain.ApiClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApiClientRepository extends JpaRepository<ApiClient, UUID> {

    Optional<ApiClient> findByClientId(String clientId);
}

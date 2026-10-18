package com.example.identity_service.services;

import com.ppms.shared.security.RoleClaim;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class HttpAdminClient implements AdminClient {

    private final RestClient restClient;

    public HttpAdminClient(@Value("${ppms.admin-service.url:http://localhost:8085}") String adminUrl) {
        this.restClient = RestClient.builder().baseUrl(adminUrl).build();
    }

    @Override
    public EffectiveAccess getEffectiveAccess(UUID userId) {
        try {
            return restClient.get()
                    .uri("/internal/users/{userId}/effective-access", userId)
                    .retrieve()
                    .body(EffectiveAccess.class);
        } catch (Exception e) {
            throw new RuntimeException("ADMIN_UNAVAILABLE");
        }
    }
}

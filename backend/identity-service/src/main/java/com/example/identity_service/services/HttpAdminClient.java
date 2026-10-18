package com.example.identity_service.services;

import com.ppms.shared.security.InternalCallSigner;
import com.ppms.shared.security.RoleClaim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class HttpAdminClient implements AdminClient {

    private static final Logger log = LoggerFactory.getLogger(HttpAdminClient.class);

    private final RestClient restClient;
    private final String internalSecret;

    public HttpAdminClient(@Value("${ppms.admin-service.url:http://localhost:8085}") String adminUrl,
                           @Value("${ppms.internal-secret:${PPMS_INTERNAL_SECRET:}}") String internalSecret) {
        this.restClient = RestClient.builder().baseUrl(adminUrl).build();
        this.internalSecret = internalSecret;
        if (internalSecret == null || internalSecret.isBlank()) {
            log.warn("PPMS_INTERNAL_SECRET is not set; calls to admin-service internal endpoints will be rejected");
        }
    }

    @Override
    public EffectiveAccess getEffectiveAccess(UUID userId) {
        try {
            String timestamp = InternalCallSigner.currentTimestamp();
            return restClient.get()
                    .uri("/internal/users/{userId}/effective-access", userId)
                    .header(InternalCallSigner.TIMESTAMP_HEADER, timestamp)
                    .header(InternalCallSigner.TOKEN_HEADER, InternalCallSigner.sign(internalSecret, timestamp))
                    .retrieve()
                    .body(EffectiveAccess.class);
        } catch (Exception e) {
            log.error("Failed to fetch effective access for user {} from admin-service: {}", userId, e.getMessage());
            throw new RuntimeException("ADMIN_UNAVAILABLE", e);
        }
    }
}

package com.example.identity_service.services;

import com.ppms.shared.security.RoleClaim;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Profile("local")
public class MockAdminClient implements AdminClient {

    @Override
    public List<RoleClaim> getEffectiveRoles(UUID userId) {
        return List.of(); // empty until shared-kernel has RoleClaim
    }
}
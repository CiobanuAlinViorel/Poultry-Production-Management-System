package com.example.identity_service.services;

import com.ppms.shared.domain.ScopeType;
import com.ppms.shared.security.RoleClaim;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Profile("local")
public class MockAdminClient implements AdminClient {

    @Override
    public EffectiveAccess getEffectiveAccess(UUID userId) {
        return new EffectiveAccess(List.of(new RoleClaim("SYSTEM_ADMIN", ScopeType.GLOBAL, null)), Set.of());
    }
}

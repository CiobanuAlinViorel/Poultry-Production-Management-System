package com.example.identity_service.services;

import com.ppms.shared.security.RoleClaim;

import java.util.List;
import java.util.UUID;

public interface AdminClient {
    List<RoleClaim> getEffectiveRoles(UUID userId);

}

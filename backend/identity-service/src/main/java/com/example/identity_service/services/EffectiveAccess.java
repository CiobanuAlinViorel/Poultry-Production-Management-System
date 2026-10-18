package com.example.identity_service.services;

import com.ppms.shared.security.RoleClaim;

import java.util.List;
import java.util.Set;

public record EffectiveAccess(
        List<RoleClaim> roles,
        Set<String> perms
) {
}

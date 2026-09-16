package com.ppms.shared.security;

import com.ppms.shared.domain.ScopeType;

import java.util.UUID;

public record RoleClaim(
        String role,          // e.g. "SYSTEM_ADMIN", "FARM_VET"
        ScopeType scopeType,  // GLOBAL, FARM_SITE, HATCHERY, FARM, SLAUGHTERHOUSE
        UUID scopeId
) {}

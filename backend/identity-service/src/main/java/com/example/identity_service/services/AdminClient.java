package com.example.identity_service.services;

import java.util.UUID;

public interface AdminClient {
    EffectiveAccess getEffectiveAccess(UUID userId);
}

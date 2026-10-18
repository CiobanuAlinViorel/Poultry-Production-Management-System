package com.example.identity_service.services;

import com.example.identity_service.domain.LoginAuditEvent;
import com.example.identity_service.domain.LoginEventReason;
import com.example.identity_service.repositories.LoginAuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoginAuditService {

    private final LoginAuditEventRepository loginAuditEventRepository;

    // Own transaction so the audit row survives even when the caller's
    // transaction rolls back after throwing BadCredentialsException.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID userId, String email, boolean success, LoginEventReason reason, String ipAddress) {
        loginAuditEventRepository.save(new LoginAuditEvent(
                userId, email, success, reason, ipAddress, Instant.now()
        ));
    }
}

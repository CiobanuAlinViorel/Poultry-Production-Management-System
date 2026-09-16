package com.example.identity_service.repositories;

import com.example.identity_service.domain.LoginAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoginAuditEventRepository extends JpaRepository<LoginAuditEvent, UUID> {

    // check audit trail for a user (admin view, investigations)
    List<LoginAuditEvent> findByUserIdOrderByOccurredAtDesc(UUID userId);

    // log attempts against unknown emails
    List<LoginAuditEvent> findByEmailOrderByOccurredAtDesc(String email);
}
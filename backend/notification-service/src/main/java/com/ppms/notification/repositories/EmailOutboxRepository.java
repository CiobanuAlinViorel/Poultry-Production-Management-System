package com.ppms.notification.repositories;

import com.ppms.notification.domain.EmailOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, java.util.UUID> {

    List<EmailOutbox> findByStatus(EmailOutbox.Status status);
}

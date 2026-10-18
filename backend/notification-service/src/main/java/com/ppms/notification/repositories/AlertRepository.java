package com.ppms.notification.repositories;

import com.ppms.notification.domain.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Optional<Alert> findByDedupKeyAndStatus(String dedupKey, Alert.Status status);
}

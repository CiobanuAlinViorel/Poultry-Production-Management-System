package com.example.identity_service.repositories;

import com.example.identity_service.domain.KioskDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KioskDeviceRepository extends JpaRepository<KioskDevice, UUID> {

    Optional<KioskDevice> findByTokenHash(String tokenHash);
}

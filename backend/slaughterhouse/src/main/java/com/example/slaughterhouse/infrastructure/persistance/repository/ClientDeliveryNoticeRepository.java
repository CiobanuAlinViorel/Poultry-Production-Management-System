package com.example.slaughterhouse.infrastructure.persistance.repository;

import com.example.slaughterhouse.domain.entities.ClientDeliveryNotice;
import com.example.slaughterhouse.domain.enums.ClientDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientDeliveryNoticeRepository extends JpaRepository<ClientDeliveryNotice, Long> {

    // Load a delivery notice together with its packages (avoiding LazyInitializationException)
    @Query("SELECT cdn FROM ClientDeliveryNotice cdn LEFT JOIN FETCH cdn.packages WHERE cdn.id = :id")
    Optional<ClientDeliveryNotice> findByIdWithPackages(@Param("id") Long id);

    // Find all delivery notices for a specific status
    List<ClientDeliveryNotice> findByStatus(ClientDeliveryStatus status);

    // Find all delivery notices for a given client name
    List<ClientDeliveryNotice> findByClientName(String clientName);

    // Load all delivery notices with packages for a specific client
    @Query("SELECT DISTINCT cdn FROM ClientDeliveryNotice cdn " +
            "LEFT JOIN FETCH cdn.packages " +
            "WHERE cdn.clientName = :clientName")
    List<ClientDeliveryNotice> findAllByClientNameWithPackages(@Param("clientName") String clientName);
}

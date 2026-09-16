package com.example.slaughterhouse.infrastructure.persistance.repository;

import com.example.slaughterhouse.domain.entities.Transport;
import com.example.slaughterhouse.domain.enums.TransportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransportRepository extends JpaRepository<Transport, Long> {

    // Load a transport together with its client delivery notice
    @Query("SELECT t FROM Transport t LEFT JOIN FETCH t.clientDeliveryNotice WHERE t.id = :id")
    Optional<Transport> findByIdWithClientDeliveryNotice(@Param("id") Long id);

    // Find all transports by delivery status
    List<Transport> findByDeliveryStatus(TransportStatus status);

    // Find transport by client delivery notice id
    @Query("SELECT t FROM Transport t WHERE t.clientDeliveryNotice.id = :noticeId")
    Optional<Transport> findByClientDeliveryNoticeId(@Param("noticeId") Long noticeId);

    // Load all transports for a specific date
    @Query("SELECT t FROM Transport t WHERE t.departureDate = :date")
    List<Transport> findAllByDepartureDate(@Param("date") java.time.LocalDate date);

    // Load all transports with a specific driver
    List<Transport> findByDriverName(String driverName);
}

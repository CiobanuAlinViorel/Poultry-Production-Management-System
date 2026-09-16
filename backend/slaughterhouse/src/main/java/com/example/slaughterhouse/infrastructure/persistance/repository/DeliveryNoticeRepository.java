package com.example.slaughterhouse.infrastructure.persistance.repository;

import com.example.slaughterhouse.domain.entities.DeliveryNotice;
import com.example.slaughterhouse.domain.entities.SlaughterhouseUser;
import com.example.slaughterhouse.domain.enums.ReceptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DeliveryNoticeRepository extends JpaRepository<DeliveryNotice, Long> {

    // Găsește un DeliveryNotice după ID-ul sistemului extern
    Optional<DeliveryNotice> findByExternalSystemId(String externalSystemId);

    // Toate DeliveryNotice active
    List<DeliveryNotice> findByIsActiveTrue();

    // DeliveryNotice după status
    List<DeliveryNotice> findByReceptionStatus(ReceptionStatus status);

    // DeliveryNotice primite de un anumit angajat
    List<DeliveryNotice> findByReceivedBy(SlaughterhouseUser receivedBy);

    // DeliveryNotice programate într-o anumită perioadă
    @Query("SELECT d FROM DeliveryNotice d WHERE d.scheduledDeliveryDate BETWEEN :startDate AND :endDate")
    List<DeliveryNotice> findScheduledBetweenDates(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    // Ultimul DeliveryNotice primit
    @Query("SELECT d FROM DeliveryNotice d ORDER BY d.receivedDate DESC")
    Optional<DeliveryNotice> findLatestReceived();
}

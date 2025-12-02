package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.DeliveryNotice;
import com.example.broilerfarm.domain.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryNoticeRepository extends JpaRepository<DeliveryNotice, Long> {

    List<DeliveryNotice> findByFarmId(Long farmId);

    List<DeliveryNotice> findByApprovalStatus(ApprovalStatus status);

    @Query("SELECT d FROM DeliveryNotice d LEFT JOIN FETCH d.deliveryLines WHERE d.id = :id")
    Optional<DeliveryNotice> findByIdWithLines(@Param("id") Long id);

    @Query("SELECT d FROM DeliveryNotice d WHERE d.approvalStatus = 'APPROVED' " +
            "AND d.transmissionTimestamp IS NULL")
    List<DeliveryNotice> findReadyToSend();

    @Query("SELECT d FROM DeliveryNotice d WHERE d.transmissionTimestamp IS NOT NULL " +
            "ORDER BY d.transmissionTimestamp DESC")
    List<DeliveryNotice> findSentDeliveries();

    @Query("SELECT d FROM DeliveryNotice d WHERE d.farm.id = :farmId " +
            "AND d.scheduledDate BETWEEN :startDate AND :endDate " +
            "ORDER BY d.scheduledDate ASC")
    List<DeliveryNotice> findByFarmIdAndScheduledDateRange(
            @Param("farmId") Long farmId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT d FROM DeliveryNotice d WHERE d.destination = :destination " +
            "ORDER BY d.scheduledDate DESC")
    List<DeliveryNotice> findByDestinationOrderByDateDesc(@Param("destination") String destination);

    @Query("SELECT d FROM DeliveryNotice d WHERE d.transportManager.id = :transportManagerId " +
            "AND d.approvalStatus = 'APPROVED' " +
            "ORDER BY d.scheduledDate ASC")
    List<DeliveryNotice> findByTransportManagerIdAndApproved(@Param("transportManagerId") Long transportManagerId);

    @Query("SELECT d FROM DeliveryNotice d WHERE d.farm.id = :farmId " +
            "AND d.approvalStatus = 'PENDING'")
    List<DeliveryNotice> findPendingApprovalByFarm(@Param("farmId") Long farmId);

    @Query("SELECT d FROM DeliveryNotice d WHERE d.scheduledDate < :cutoffDate " +
            "AND d.approvalStatus = 'APPROVED' " +
            "AND d.transmissionTimestamp IS NULL")
    List<DeliveryNotice> findOverdueDeliveries(@Param("cutoffDate") LocalDateTime cutoffDate);

    // ✅ Statistics queries
    @Query("SELECT COUNT(d) FROM DeliveryNotice d WHERE d.farm.id = :farmId " +
            "AND d.transmissionTimestamp BETWEEN :startDate AND :endDate")
    Long countDeliveriesByFarmAndDateRange(
            @Param("farmId") Long farmId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );


}
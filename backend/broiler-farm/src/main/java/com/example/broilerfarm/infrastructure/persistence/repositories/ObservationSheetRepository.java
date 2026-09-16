package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.ObservationSheet;
import com.example.broilerfarm.domain.enums.ObservationSheetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ObservationSheetRepository extends JpaRepository<ObservationSheet, Long> {

    // Simple query - Spring Data derives the query
    List<ObservationSheet> findByStatus(ObservationSheetStatus status);

    // FIXED: Changed lot.lot_number to lot.lotNumber
    @Query("SELECT o FROM ObservationSheet o WHERE o.lot.lotNumber = :lotNumber " +
            "ORDER BY o.weekNumber DESC")
    List<ObservationSheet> findByLotIdOrderByWeekNumberDesc(@Param("lotNumber") String lotNumber);

    // FIXED: Changed lot.lot_number to lot.lotNumber
    @Query("SELECT o FROM ObservationSheet o WHERE o.lot.lotNumber = :lotNumber " +
            "AND o.status = 'APPROVED' " +
            "ORDER BY o.weekNumber DESC")
    Optional<ObservationSheet> findLatestApprovedByLotId(@Param("lotNumber") String lotNumber);

    // FIXED: Changed lot.lot_number to lot.lotNumber
    @Query("SELECT o FROM ObservationSheet o WHERE o.lot.lotNumber = :lotNumber " +
            "AND o.weekNumber = :weekNumber - 1 " +
            "AND o.status = 'APPROVED'")
    Optional<ObservationSheet> findPreviousWeekObservation(
            @Param("lotNumber") String lotNumber,
            @Param("weekNumber") Integer weekNumber
    );

    // Farm queries
    @Query("SELECT o FROM ObservationSheet o WHERE o.lot.house.farm.id = :farmId " +
            "AND o.endDate BETWEEN :startDate AND :endDate")
    List<ObservationSheet> findByFarmIdAndDateRange(
            @Param("farmId") Long farmId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Approval queries
    @Query("SELECT o FROM ObservationSheet o WHERE o.status = 'SUBMITTED'")
    List<ObservationSheet> findPendingApproval();

    @Query("SELECT o FROM ObservationSheet o WHERE o.lot.house.farm.id = :farmId " +
            "AND o.status = 'SUBMITTED'")
    List<ObservationSheet> findPendingApprovalByFarm(@Param("farmId") Long farmId);

    // Performance analytics queries
    @Query("SELECT o FROM ObservationSheet o WHERE o.status = 'APPROVED' " +
            "AND o.fcr > :threshold")
    List<ObservationSheet> findObservationsWithHighFCR(@Param("threshold") java.math.BigDecimal threshold);

    @Query("SELECT AVG(o.fcr) FROM ObservationSheet o WHERE o.lot.house.farm.id = :farmId " +
            "AND o.status = 'APPROVED' " +
            "AND o.endDate BETWEEN :startDate AND :endDate")
    java.math.BigDecimal getAverageFCRByFarmAndDateRange(
            @Param("farmId") Long farmId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT AVG(o.averageWeight) FROM ObservationSheet o WHERE o.lot.house.farm.id = :farmId " +
            "AND o.status = 'APPROVED' " +
            "AND o.weekNumber = :weekNumber")
    java.math.BigDecimal getAverageWeightByFarmAndWeek(
            @Param("farmId") Long farmId,
            @Param("weekNumber") Integer weekNumber
    );

    // Existence check - FIXED: Use correct property name
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM ObservationSheet o " +
            "WHERE o.lot.lotNumber = :lotNumber AND o.weekNumber = :weekNumber")
    boolean existsByLotIdAndWeekNumber(
            @Param("lotNumber") String lotNumber,
            @Param("weekNumber") Integer weekNumber
    );
}
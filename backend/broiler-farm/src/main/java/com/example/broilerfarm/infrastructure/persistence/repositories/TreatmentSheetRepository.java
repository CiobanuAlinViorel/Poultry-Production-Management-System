package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.TreatmentSheet;
import com.example.broilerfarm.domain.enums.TreatmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TreatmentSheetRepository extends JpaRepository<TreatmentSheet, Long> {

    // Simple queries
    List<TreatmentSheet> findByStatus(TreatmentStatus status);

    // Fetch with lines (eager loading)
    @Query("SELECT t FROM TreatmentSheet t LEFT JOIN FETCH t.treatmentLines WHERE t.id = :id")
    Optional<TreatmentSheet> findByIdWithLines(@Param("id") Long id);

    // Find by lot and statuses - FIXED: use t.lot.lotNumber instead of t.lot_number
    @Query("SELECT t FROM TreatmentSheet t WHERE t.lot.lotNumber = :lotNumber AND t.status IN :statuses")
    List<TreatmentSheet> findByLotIdAndStatusIn(
            @Param("lotNumber") String lotNumber,
            @Param("statuses") List<TreatmentStatus> statuses
    );

    // Find active treatments by lot - FIXED
    @Query("SELECT t FROM TreatmentSheet t WHERE t.lot.lotNumber = :lotNumber " +
            "AND t.status = 'ACTIVE' " +
            "ORDER BY t.treatmentDate DESC")
    List<TreatmentSheet> findActiveTreatmentsByLot(@Param("lotNumber") String lotNumber);

    // Find active treatments by farm - FIXED
    @Query("SELECT t FROM TreatmentSheet t WHERE t.lot.house.farm.id = :farmId " +
            "AND t.status = 'ACTIVE'")
    List<TreatmentSheet> findActiveTreatmentsByFarm(@Param("farmId") Long farmId);

    // Find by lot and date range - FIXED
    @Query("SELECT t FROM TreatmentSheet t WHERE t.lot.lotNumber = :lotNumber " +
            "AND t.treatmentDate BETWEEN :startDate AND :endDate")
    List<TreatmentSheet> findByLotIdAndDateRange(
            @Param("lotNumber") String lotNumber,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Find by veterinarian
    @Query("SELECT t FROM TreatmentSheet t WHERE t.veterinarian.id = :vetId " +
            "ORDER BY t.treatmentDate DESC")
    List<TreatmentSheet> findByVeterinarianIdOrderByDateDesc(@Param("vetId") Long vetId);

    // Find draft treatments by farm
    @Query("SELECT t FROM TreatmentSheet t WHERE t.lot.house.farm.id = :farmId " +
            "AND t.status = 'DRAFT'")
    List<TreatmentSheet> findDraftTreatmentsByFarm(@Param("farmId") Long farmId);

    /**
     * CRITICAL FOR WITHDRAWAL PERIOD CHECKS
     * Returns all treatments (ACTIVE or COMPLETED) with lines eagerly fetched
     * Used by TreatmentWithdrawalService to check withdrawal periods
     */
    @Query("SELECT DISTINCT t FROM TreatmentSheet t " +
            "LEFT JOIN FETCH t.treatmentLines tl " +
            "WHERE t.lot.lotNumber = :lotNumber " +
            "AND t.status IN ('ACTIVE', 'COMPLETED') " +
            "ORDER BY t.treatmentDate DESC")
    List<TreatmentSheet> findTreatmentsForWithdrawalCheck(@Param("lotNumber") String lotNumber);

    // Search by diagnosis or reason
    @Query("SELECT t FROM TreatmentSheet t WHERE t.diagnosis LIKE %:keyword% " +
            "OR t.treatmentReason LIKE %:keyword%")
    List<TreatmentSheet> searchByDiagnosisOrReason(@Param("keyword") String keyword);
}
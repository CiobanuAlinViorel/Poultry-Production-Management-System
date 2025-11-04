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

    List<TreatmentSheet> findByLotId(Long lotId);

    List<TreatmentSheet> findByStatus(TreatmentStatus status);

    @Query("SELECT t FROM TreatmentSheet t LEFT JOIN FETCH t.treatmentLines WHERE t.id = :id")
    Optional<TreatmentSheet> findByIdWithLines(@Param("id") Long id);

    @Query("SELECT t FROM TreatmentSheet t WHERE t.lot.id = :lotId AND t.status IN :statuses")
    List<TreatmentSheet> findByLotIdAndStatusIn(
            @Param("lotId") Long lotId,
            @Param("statuses") List<TreatmentStatus> statuses
    );

    @Query("SELECT t FROM TreatmentSheet t WHERE t.lot.id = :lotId " +
            "AND t.status = 'ACTIVE' " +
            "ORDER BY t.treatmentDate DESC")
    List<TreatmentSheet> findActiveTreatmentsByLot(@Param("lotId") Long lotId);

    @Query("SELECT t FROM TreatmentSheet t WHERE t.lot.house.farm.id = :farmId " +
            "AND t.status = 'ACTIVE'")
    List<TreatmentSheet> findActiveTreatmentsByFarm(@Param("farmId") Long farmId);

    @Query("SELECT t FROM TreatmentSheet t WHERE t.lot.id = :lotId " +
            "AND t.treatmentDate BETWEEN :startDate AND :endDate")
    List<TreatmentSheet> findByLotIdAndDateRange(
            @Param("lotId") Long lotId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT t FROM TreatmentSheet t WHERE t.veterinarian.id = :vetId " +
            "ORDER BY t.treatmentDate DESC")
    List<TreatmentSheet> findByVeterinarianIdOrderByDateDesc(@Param("vetId") Long vetId);

    @Query("SELECT t FROM TreatmentSheet t WHERE t.lot.house.farm.id = :farmId " +
            "AND t.status = 'DRAFT'")
    List<TreatmentSheet> findDraftTreatmentsByFarm(@Param("farmId") Long farmId);

    // ✅ CORECT: Returnează toate treatment sheets active/completed
    // Verificarea withdrawal period se face în Domain Service
    @Query("SELECT t FROM TreatmentSheet t LEFT JOIN FETCH t.treatmentLines tl " +
            "WHERE t.lot.id = :lotId " +
            "AND t.status IN ('ACTIVE', 'COMPLETED')")
    List<TreatmentSheet> findTreatmentsForWithdrawalCheck(@Param("lotId") Long lotId);

    @Query("SELECT t FROM TreatmentSheet t WHERE t.diagnosis LIKE %:keyword% " +
            "OR t.treatmentReason LIKE %:keyword%")
    List<TreatmentSheet> searchByDiagnosisOrReason(@Param("keyword") String keyword);
}
package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.MortalitySheet;
import com.example.broilerfarm.domain.enums.MortalitySheetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MortalitySheetRepository extends JpaRepository<MortalitySheet, Long> {

    // Simple queries
    List<MortalitySheet> findByStatus(MortalitySheetStatus status);

    // FIXED: Changed lot.lot_number to lot.lotNumber
    @Query("SELECT m FROM MortalitySheet m WHERE m.lot.lotNumber = :lotNumber " +
            "AND m.sheetDate BETWEEN :startDate AND :endDate " +
            "ORDER BY m.sheetDate ASC")
    List<MortalitySheet> findByLotIdAndDateRange(
            @Param("lotNumber") String lotNumber,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // FIXED: Changed lot.lot_number to lot.lotNumber
    @Query("SELECT m FROM MortalitySheet m WHERE m.lot.lotNumber = :lotNumber " +
            "ORDER BY m.sheetDate DESC")
    List<MortalitySheet> findByLotIdOrderByDateDesc(@Param("lotNumber") String lotNumber);

    // Farm queries
    @Query("SELECT m FROM MortalitySheet m WHERE m.lot.house.farm.id = :farmId " +
            "AND m.sheetDate = :date")
    List<MortalitySheet> findByFarmIdAndDate(
            @Param("farmId") Long farmId,
            @Param("date") LocalDate date
    );

    // Statistics
    @Query("SELECT SUM(m.totalMortality) FROM MortalitySheet m WHERE m.lot.lotNumber = :lotNumber " +
            "AND m.status = 'APPROVED'")
    Integer getTotalMortalityByLot(@Param("lotNumber") String lotNumber);

    // Approval queries
    @Query("SELECT m FROM MortalitySheet m WHERE m.status = 'SUBMITTED'")
    List<MortalitySheet> findPendingApproval();

    @Query("SELECT m FROM MortalitySheet m WHERE m.lot.house.farm.id = :farmId " +
            "AND m.status = 'SUBMITTED'")
    List<MortalitySheet> findPendingApprovalByFarm(@Param("farmId") Long farmId);

    // Existence check - FIXED: Add @Query since lot doesn't have 'id' property
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM MortalitySheet m " +
            "WHERE m.lot.lotNumber = :lotNumber AND m.sheetDate = :date")
    boolean existsByLotIdAndSheetDate(
            @Param("lotNumber") String lotNumber,
            @Param("date") LocalDate date
    );
}
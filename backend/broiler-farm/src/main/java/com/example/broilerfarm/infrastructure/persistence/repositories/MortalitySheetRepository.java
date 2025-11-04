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

    List<MortalitySheet> findByLotId(Long lotId);

    Optional<MortalitySheet> findByLotIdAndSheetDate(Long lotId, LocalDate date);

    List<MortalitySheet> findByStatus(MortalitySheetStatus status);

    @Query("SELECT m FROM MortalitySheet m WHERE m.lot.id = :lotId " +
            "AND m.sheetDate BETWEEN :startDate AND :endDate " +
            "ORDER BY m.sheetDate ASC")
    List<MortalitySheet> findByLotIdAndDateRange(
            @Param("lotId") Long lotId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT m FROM MortalitySheet m WHERE m.lot.house.farm.id = :farmId " +
            "AND m.sheetDate = :date")
    List<MortalitySheet> findByFarmIdAndDate(
            @Param("farmId") Long farmId,
            @Param("date") LocalDate date
    );

    @Query("SELECT m FROM MortalitySheet m WHERE m.lot.id = :lotId " +
            "ORDER BY m.sheetDate DESC")
    List<MortalitySheet> findByLotIdOrderByDateDesc(@Param("lotId") Long lotId);

    @Query("SELECT SUM(m.totalMortality) FROM MortalitySheet m WHERE m.lot.id = :lotId " +
            "AND m.status = 'APPROVED'")
    Integer getTotalMortalityByLot(@Param("lotId") Long lotId);

    @Query("SELECT m FROM MortalitySheet m WHERE m.status = 'SUBMITTED'")
    List<MortalitySheet> findPendingApproval();

    @Query("SELECT m FROM MortalitySheet m WHERE m.lot.house.farm.id = :farmId " +
            "AND m.status = 'SUBMITTED'")
    List<MortalitySheet> findPendingApprovalByFarm(@Param("farmId") Long farmId);

    boolean existsByLotIdAndSheetDate(Long lotId, LocalDate date);
}
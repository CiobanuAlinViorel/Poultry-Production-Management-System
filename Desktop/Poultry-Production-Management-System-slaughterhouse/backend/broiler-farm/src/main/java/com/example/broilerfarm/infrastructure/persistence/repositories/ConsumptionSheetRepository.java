package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.ConsumptionSheet;
import com.example.broilerfarm.domain.enums.ConsumptionSheetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsumptionSheetRepository extends JpaRepository<ConsumptionSheet, Long> {

    // Simple queries
    List<ConsumptionSheet> findByStatus(ConsumptionSheetStatus status);

    // Fetch with lines (eager loading)
    @Query("SELECT cs FROM ConsumptionSheet cs LEFT JOIN FETCH cs.consumptionLines WHERE cs.id = :id")
    Optional<ConsumptionSheet> findByIdWithLines(@Param("id") Long id);

    // FIXED: Changed lot.lot_number to lot.lotNumber
    @Query("SELECT cs FROM ConsumptionSheet cs WHERE cs.lot.lotNumber = :lotNumber " +
            "AND cs.sheetDate BETWEEN :startDate AND :endDate " +
            "ORDER BY cs.sheetDate ASC")
    List<ConsumptionSheet> findByLotIdAndDateRange(
            @Param("lotNumber") String lotNumber,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Farm queries
    @Query("SELECT cs FROM ConsumptionSheet cs WHERE cs.lot.house.farm.id = :farmId " +
            "AND cs.sheetDate = :date")
    List<ConsumptionSheet> findByFarmIdAndDate(
            @Param("farmId") Long farmId,
            @Param("date") LocalDate date
    );

    // Approval queries
    @Query("SELECT cs FROM ConsumptionSheet cs WHERE cs.status = 'SUBMITTED'")
    List<ConsumptionSheet> findPendingApproval();

    @Query("SELECT cs FROM ConsumptionSheet cs WHERE cs.lot.house.farm.id = :farmId " +
            "AND cs.status = 'SUBMITTED'")
    List<ConsumptionSheet> findPendingApprovalByFarm(@Param("farmId") Long farmId);

    // Existence check - FIXED: Add @Query
    @Query("SELECT CASE WHEN COUNT(cs) > 0 THEN true ELSE false END FROM ConsumptionSheet cs " +
            "WHERE cs.lot.lotNumber = :lotNumber AND cs.sheetDate = :date")
    boolean existsByLotIdAndSheetDate(
            @Param("lotNumber") String lotNumber,
            @Param("date") LocalDate date
    );
}
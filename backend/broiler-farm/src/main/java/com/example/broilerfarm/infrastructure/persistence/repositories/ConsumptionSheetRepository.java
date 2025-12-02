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

    List<ConsumptionSheet> findByLotId(Long lotId);

    Optional<ConsumptionSheet> findByLotIdAndSheetDate(Long lotId, LocalDate date);

    List<ConsumptionSheet> findByStatus(ConsumptionSheetStatus status);

    @Query("SELECT cs FROM ConsumptionSheet cs LEFT JOIN FETCH cs.consumptionLines WHERE cs.id = :id")
    Optional<ConsumptionSheet> findByIdWithLines(@Param("id") Long id);

    @Query("SELECT cs FROM ConsumptionSheet cs WHERE cs.lot.id = :lotId " +
            "AND cs.sheetDate BETWEEN :startDate AND :endDate " +
            "ORDER BY cs.sheetDate ASC")
    List<ConsumptionSheet> findByLotIdAndDateRange(
            @Param("lotId") Long lotId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT cs FROM ConsumptionSheet cs WHERE cs.lot.house.farm.id = :farmId " +
            "AND cs.sheetDate = :date")
    List<ConsumptionSheet> findByFarmIdAndDate(
            @Param("farmId") Long farmId,
            @Param("date") LocalDate date
    );

    @Query("SELECT cs FROM ConsumptionSheet cs WHERE cs.status = 'SUBMITTED'")
    List<ConsumptionSheet> findPendingApproval();

    @Query("SELECT cs FROM ConsumptionSheet cs WHERE cs.lot.house.farm.id = :farmId " +
            "AND cs.status = 'SUBMITTED'")
    List<ConsumptionSheet> findPendingApprovalByFarm(@Param("farmId") Long farmId);

    boolean existsByLotIdAndSheetDate(Long lotId, LocalDate date);


}
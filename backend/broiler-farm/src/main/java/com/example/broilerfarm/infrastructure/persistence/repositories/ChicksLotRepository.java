package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.ChicksLot;
import com.example.broilerfarm.domain.enums.ChicksLotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChicksLotRepository extends JpaRepository<ChicksLot, String> {

    ChicksLot findByLotNumber(String lotNumber);

    List<ChicksLot> findByStatus(ChicksLotStatus status);

    List<ChicksLot> findByHouseId(Long houseId);

    Optional<ChicksLot> findByHouseIdAndStatus(Long houseId, ChicksLotStatus status);

    @Query("SELECT c FROM ChicksLot c WHERE c.house.farm.id = :farmId")
    List<ChicksLot> findByFarmId(@Param("farmId") Long farmId);

    @Query("SELECT c FROM ChicksLot c WHERE c.house.farm.id = :farmId AND c.status = :status")
    List<ChicksLot> findByFarmIdAndStatus(
            @Param("farmId") Long farmId,
            @Param("status") ChicksLotStatus status
    );

    @Query("SELECT c FROM ChicksLot c WHERE c.status IN ('GROWING', 'READY_FOR_DELIVERY')")
    List<ChicksLot> findActiveLots();

    @Query("SELECT c FROM ChicksLot c WHERE c.house.farm.id = :farmId " +
            "AND c.status IN ('GROWING', 'READY_FOR_DELIVERY')")
    List<ChicksLot> findActiveLotsForFarm(@Param("farmId") Long farmId);

    @Query("SELECT c FROM ChicksLot c WHERE c.receptionDate BETWEEN :startDate AND :endDate")
    List<ChicksLot> findByReceptionDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query(value = "SELECT * FROM chicks_lots cl " +
            "WHERE cl.status = 'READY_FOR_DELIVERY' " +
            "AND DATEDIFF(DAY, cl.ready_date, CURRENT_DATE) >= :minDays",
            nativeQuery = true)
    List<ChicksLot> findLotsEligibleForDelivery(@Param("minDays") int minDays);

    @Query("SELECT COUNT(c) FROM ChicksLot c WHERE c.house.farm.id = :farmId " +
            "AND c.status IN ('GROWING', 'READY_FOR_DELIVERY')")
    Long countActiveLotsForFarm(@Param("farmId") Long farmId);

    @Query("SELECT SUM(c.currentQuantity) FROM ChicksLot c WHERE c.house.farm.id = :farmId " +
            "AND c.status IN ('GROWING', 'READY_FOR_DELIVERY')")
    Integer getTotalBirdsAliveByFarm(@Param("farmId") Long farmId);

    boolean existsByLotNumber(String lotNumber);
}
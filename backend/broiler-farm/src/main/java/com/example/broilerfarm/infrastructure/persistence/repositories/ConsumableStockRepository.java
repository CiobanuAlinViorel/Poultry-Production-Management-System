package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.ConsumableStock;
import com.example.broilerfarm.domain.enums.StockStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsumableStockRepository extends JpaRepository<ConsumableStock, Long> {

    List<ConsumableStock> findByWarehouseId(Long warehouseId);

    List<ConsumableStock> findByConsumableId(Long consumableId);

    List<ConsumableStock> findByStatus(StockStatus status);

    @Query("SELECT cs FROM ConsumableStock cs WHERE cs.consumable.id = :consumableId " +
            "AND cs.warehouse.id = :warehouseId")
    List<ConsumableStock> findByConsumableIdAndWarehouseId(
            @Param("consumableId") Long consumableId,
            @Param("warehouseId") Long warehouseId
    );

    // ✅ CRITICAL: FIFO - First In, First Out by expiration date
    @Query("SELECT cs FROM ConsumableStock cs WHERE cs.consumable.id = :consumableId " +
            "AND cs.warehouse.id = :warehouseId " +
            "AND cs.status = 'AVAILABLE' " +
            "AND (cs.quantityOnHand - cs.reservedQuantity) > 0 " +
            "ORDER BY cs.expirationDate ASC, cs.manufacturingDate ASC")
    List<ConsumableStock> findAvailableStockFIFO(
            @Param("consumableId") Long consumableId,
            @Param("warehouseId") Long warehouseId
    );

    @Query("SELECT cs FROM ConsumableStock cs WHERE cs.warehouse.farm.id = :farmId " +
            "AND (cs.quantityOnHand - cs.reservedQuantity) < cs.consumable.reorderPoint")
    List<ConsumableStock> findStockBelowReorderPoint(@Param("farmId") Long farmId);

    @Query("SELECT cs FROM ConsumableStock cs WHERE cs.expirationDate <= :date " +
            "AND cs.status != 'EXPIRED'")
    List<ConsumableStock> findExpiredStock(@Param("date") LocalDate date);

    @Query("SELECT cs FROM ConsumableStock cs WHERE cs.expirationDate BETWEEN :startDate AND :endDate " +
            "AND cs.status = 'AVAILABLE'")
    List<ConsumableStock> findStockExpiringSoon(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT SUM(cs.quantityOnHand - cs.reservedQuantity) FROM ConsumableStock cs " +
            "WHERE cs.consumable.id = :consumableId " +
            "AND cs.warehouse.farm.id = :farmId " +
            "AND cs.status = 'AVAILABLE'")
    BigDecimal getTotalAvailableQuantity(
            @Param("consumableId") Long consumableId,
            @Param("farmId") Long farmId
    );

    @Query("SELECT cs FROM ConsumableStock cs WHERE cs.batchNumber = :batchNumber")
    List<ConsumableStock> findByBatchNumber(@Param("batchNumber") String batchNumber);
}
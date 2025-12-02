package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.ConsumableReception;
import com.example.broilerfarm.domain.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsumableReceptionRepository extends JpaRepository<ConsumableReception, Long> {

    List<ConsumableReception> findByReceivingWarehouseId(Long warehouseId);

    Optional<ConsumableReception> findByPurchaseOrderRef(String poRef);

    List<ConsumableReception> findByApprovalStatus(ApprovalStatus status);

    @Query("SELECT cr FROM ConsumableReception cr LEFT JOIN FETCH cr.receptionLines WHERE cr.id = :id")
    Optional<ConsumableReception> findByIdWithLines(@Param("id") Long id);

    @Query("SELECT cr FROM ConsumableReception cr WHERE cr.receivingWarehouse.farm.id = :farmId " +
            "AND cr.receptionDate BETWEEN :startDate AND :endDate")
    List<ConsumableReception> findByFarmIdAndDateRange(
            @Param("farmId") Long farmId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT cr FROM ConsumableReception cr WHERE cr.supplier = :supplier " +
            "ORDER BY cr.receptionDate DESC")
    List<ConsumableReception> findBySupplierOrderByDateDesc(@Param("supplier") String supplier);

    boolean existsByPurchaseOrderRef(String poRef);


}
package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.Warehouse;
import com.example.broilerfarm.domain.enums.WarehouseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findByFarmId(Long farmId);

    Optional<Warehouse> findByWarehouseCode(String warehouseCode);

    List<Warehouse> findByType(WarehouseType type);

    @Query("SELECT w FROM Warehouse w WHERE w.farm.id = :farmId AND w.type = :type")
    List<Warehouse> findByFarmIdAndType(
            @Param("farmId") Long farmId,
            @Param("type") WarehouseType type
    );

    @Query("SELECT w FROM Warehouse w WHERE w.currentOccupancy < w.capacity")
    List<Warehouse> findWarehousesWithAvailableCapacity();

    @Query("SELECT w FROM Warehouse w WHERE " +
            "(w.currentOccupancy / w.capacity) * 100 >= :threshold")
    List<Warehouse> findWarehousesNearCapacity(@Param("threshold") Double threshold);

    boolean existsByWarehouseCode(String warehouseCode);
}
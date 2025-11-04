package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.PoultryHouse;
import com.example.broilerfarm.domain.enums.PoultryHouseStatus;
import com.example.broilerfarm.domain.enums.PoultryHouseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PoultryHouseRepository extends JpaRepository<PoultryHouse, Long> {

    List<PoultryHouse> findByFarmId(Long farmId);

    List<PoultryHouse> findByStatus(PoultryHouseStatus status);

    List<PoultryHouse> findByType(PoultryHouseType type);

    @Query("SELECT p FROM PoultryHouse p WHERE p.farm.id = :farmId AND p.status = :status")
    List<PoultryHouse> findByFarmIdAndStatus(
            @Param("farmId") Long farmId,
            @Param("status") PoultryHouseStatus status
    );

    @Query("SELECT p FROM PoultryHouse p WHERE p.farm.id = :farmId AND p.status = 'EMPTY'")
    List<PoultryHouse> findAvailableHousesByFarm(@Param("farmId") Long farmId);

    @Query("SELECT p FROM PoultryHouse p WHERE p.currentOccupancy < p.capacity")
    List<PoultryHouse> findHousesWithAvailableCapacity();

    @Query("SELECT SUM(p.capacity) FROM PoultryHouse p WHERE p.farm.id = :farmId")
    Integer getTotalCapacityByFarm(@Param("farmId") Long farmId);

    @Query("SELECT SUM(p.currentOccupancy) FROM PoultryHouse p WHERE p.farm.id = :farmId")
    Integer getTotalOccupancyByFarm(@Param("farmId") Long farmId);
}
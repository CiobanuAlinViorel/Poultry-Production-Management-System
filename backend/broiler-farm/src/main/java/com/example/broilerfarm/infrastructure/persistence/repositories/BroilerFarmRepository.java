package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.BroilerFarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BroilerFarmRepository extends JpaRepository<BroilerFarm, Long> {

    Optional<BroilerFarm> findByLicenseNumber(String licenseNumber);

    Optional<BroilerFarm> findByFarmName(String farmName);

    boolean existsByLicenseNumber(String licenseNumber);

    @Query("SELECT f FROM BroilerFarm f LEFT JOIN FETCH f.employees WHERE f.id = :id")
    Optional<BroilerFarm> findByIdWithEmployees(@Param("id") Long id);


    @Query("SELECT f FROM BroilerFarm f WHERE f.location = :location")
    java.util.List<BroilerFarm> findByLocation(@Param("location") String location);
}
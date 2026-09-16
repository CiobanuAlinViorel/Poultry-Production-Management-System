package com.example.slaughterhouse.infrastructure.persistance.repository;

import com.example.slaughterhouse.domain.entities.Package;
import com.example.slaughterhouse.domain.enums.PackageStatus;
import com.example.slaughterhouse.domain.enums.PackageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PackageRepository extends JpaRepository<Package, Long> {

    // Caută după cod complet: prefix + number + suffix
    @Query("""
            SELECT p FROM Package p
            WHERE p.packageCode.prefix = :prefix
              AND p.packageCode.number = :number
              AND p.packageCode.suffix = :suffix
           """)
    Package findByPackageCode(
            @Param("prefix") String prefix,
            @Param("number") Integer number,
            @Param("suffix") String suffix
    );

    // Caută toate pachetele active
    List<Package> findByIsActiveTrue();

    // Caută după status (ex: PACKAGED, IN_STORAGE, READY_FOR_DELIVERY)
    List<Package> findByStatus(PackageStatus status);

    // Caută după tip (ex: FRESH, FROZEN)
    List<Package> findByPackageType(PackageType type);

    // Pachete expirate
    @Query("""
            SELECT p FROM Package p
            WHERE p.expiryDate < :today
            """)
    List<Package> findExpired(@Param("today") LocalDate today);

    // Pachete care expiră în X zile
    @Query("""
            SELECT p FROM Package p
            WHERE p.expiryDate BETWEEN :today AND :limitDate
            """)
    List<Package> findExpiringSoon(
            @Param("today") LocalDate today,
            @Param("limitDate") LocalDate limitDate
    );

}

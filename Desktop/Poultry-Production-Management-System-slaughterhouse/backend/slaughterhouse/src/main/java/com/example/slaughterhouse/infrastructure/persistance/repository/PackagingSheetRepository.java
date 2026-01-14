package com.example.slaughterhouse.infrastructure.persistance.repository;

import com.example.slaughterhouse.domain.entities.PackagingSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PackagingSheetRepository extends JpaRepository<PackagingSheet, Long> {

    /**
     * Fetch a PackagingSheet along with its packages using JPQL
     */
    @Query("SELECT p FROM PackagingSheet p LEFT JOIN FETCH p.packages WHERE p.id = :id")
    Optional<PackagingSheet> findByIdWithPackages(@Param("id") Long id);

}

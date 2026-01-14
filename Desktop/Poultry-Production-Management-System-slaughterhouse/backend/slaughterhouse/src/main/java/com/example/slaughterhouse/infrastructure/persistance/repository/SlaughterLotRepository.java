package com.example.slaughterhouse.infrastructure.persistance.repository;

import com.example.slaughterhouse.domain.entities.SlaughterLot;
import com.example.slaughterhouse.domain.enums.LotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlaughterLotRepository extends JpaRepository<SlaughterLot, Long> {

    // Căutare lot după numărul de lot (unic)
    Optional<SlaughterLot> findByLotNumber(String lotNumber);

    // Căutare loturi după manager (folosind ID-ul managerului)
    List<SlaughterLot> findByManagerId(Long managerId);

    // Căutare loturi după status (cu enum)
    List<SlaughterLot> findByStatus(LotStatus status);

    // Căutare loturi active
    List<SlaughterLot> findByIsActiveTrue();

    // Căutare loturi după data de sacrificare
    List<SlaughterLot> findBySlaughterDateBetween(LocalDate startDate, LocalDate endDate);

    // Căutare loturi după rasă
    List<SlaughterLot> findByBreed(String breed);

}
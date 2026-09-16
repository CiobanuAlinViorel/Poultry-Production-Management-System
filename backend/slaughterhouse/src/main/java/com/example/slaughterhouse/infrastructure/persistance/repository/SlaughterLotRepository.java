package com.example.slaughterhouse.infrastructure.persistance.repository;

import com.example.slaughterhouse.domain.entities.SlaughterLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SlaughterLotRepository extends JpaRepository<SlaughterLot, Long> {

    // Căutare loturi după manager (folosind ID-ul managerului)
    List<SlaughterLot> findByManagerId(Long managerId);

    // Alte metode custom pot fi adăugate aici, de exemplu:
    List<SlaughterLot> findByStatus(String status);

    List<SlaughterLot> findBySlaughterDateBetween(java.time.LocalDate startDate, java.time.LocalDate endDate);

}

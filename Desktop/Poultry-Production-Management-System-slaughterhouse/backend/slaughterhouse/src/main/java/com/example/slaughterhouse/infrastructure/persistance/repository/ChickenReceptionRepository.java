package com.example.slaughterhouse.infrastructure.persistance.repository;

import com.example.slaughterhouse.domain.entities.ChickenReception;
import com.example.slaughterhouse.domain.entities.SlaughterLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChickenReceptionRepository extends JpaRepository<ChickenReception, Long> {

    Optional<ChickenReception> findBySlaughterLot(SlaughterLot slaughterLot);

    List<ChickenReception> findByReceptionDateBetween(LocalDate startDate, LocalDate endDate);

    List<ChickenReception> findByReceivedByEmployeeId(Long employeeId);

    @Query("SELECT c FROM ChickenReception c WHERE c.isActive = true")
    List<ChickenReception> findAllActive();

    @Query("SELECT c FROM ChickenReception c WHERE c.animalWelfareCheck = false AND c.isActive = true")
    List<ChickenReception> findPendingWelfareCheck();

    @Query("SELECT c FROM ChickenReception c WHERE (CAST(c.chicksDOA AS float) / c.quantityReceived * 100) > :threshold")
    List<ChickenReception> findHighMortalityReceptions(@Param("threshold") Float threshold);

    @Query("SELECT SUM(c.quantityReceived) FROM ChickenReception c WHERE c.receptionDate BETWEEN :startDate AND :endDate")
    Integer getTotalReceivedBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT AVG(CAST(c.chicksDOA AS float) / c.quantityReceived * 100) FROM ChickenReception c WHERE c.receptionDate BETWEEN :startDate AND :endDate")
    Float getAverageMortalityRate(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<ChickenReception> findByIsActiveTrue();
}
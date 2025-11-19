package com.example.slaughterhouse.infrastructure.persistance.repository;

import com.example.slaughterhouse.domain.entities.DailyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyStatsRepository extends JpaRepository<DailyStats, Long> {

    Optional<DailyStats> findByRecordDate(LocalDate recordDate);

    List<DailyStats> findByRecordDateBetween(LocalDate startDate, LocalDate endDate);

    List<DailyStats> findByManagerEmployeeId(Long managerId);

    @Query("SELECT d FROM DailyStats d WHERE d.isActive = true")
    List<DailyStats> findAllActive();

    @Query("SELECT d FROM DailyStats d WHERE d.recordDate BETWEEN :startDate AND :endDate ORDER BY d.recordDate DESC")
    List<DailyStats> findByDateRangeOrderByDateDesc(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(d.totalChickensReceived) FROM DailyStats d WHERE d.recordDate BETWEEN :startDate AND :endDate")
    Integer getTotalChickensReceivedInPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(d.totalProcessed) FROM DailyStats d WHERE d.recordDate BETWEEN :startDate AND :endDate")
    Integer getTotalProcessedInPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT AVG(d.yieldPercentage) FROM DailyStats d WHERE d.recordDate BETWEEN :startDate AND :endDate")
    Float getAverageYieldPercentage(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(d.totalYield) FROM DailyStats d WHERE d.recordDate BETWEEN :startDate AND :endDate")
    Float getTotalYieldInPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT d FROM DailyStats d WHERE d.yieldPercentage < :threshold AND d.recordDate BETWEEN :startDate AND :endDate")
    List<DailyStats> findLowYieldDays(@Param("threshold") Float threshold, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
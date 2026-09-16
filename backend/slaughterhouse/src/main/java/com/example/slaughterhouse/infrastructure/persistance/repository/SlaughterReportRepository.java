package com.example.slaughterhouse.infrastructure.persistance.repository;

import com.example.slaughterhouse.domain.entities.SlaughterReport;
import com.example.slaughterhouse.domain.entities.SlaughterLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlaughterReportRepository extends JpaRepository<SlaughterReport, Long> {

    Optional<SlaughterReport> findBySlaughterLot(SlaughterLot slaughterLot);

    List<SlaughterReport> findByReportDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT s FROM SlaughterReport s WHERE s.isActive = TRUE")
    List<SlaughterReport> findAllActive();
    @Query("""
           SELECT s 
           FROM SlaughterReport s 
           WHERE s.reportPeriod.startDate >= :startDate 
             AND s.reportPeriod.endDate <= :endDate
           """)
    List<SlaughterReport> findByPeriodRange(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    @Query("""
           SELECT SUM(s.totalProcessed) 
           FROM SlaughterReport s 
           WHERE s.reportDate BETWEEN :startDate AND :endDate
           """)
    Integer getTotalProcessedInPeriod(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    @Query("""
           SELECT SUM(s.approved) 
           FROM SlaughterReport s 
           WHERE s.reportDate BETWEEN :startDate AND :endDate
           """)
    Integer getTotalApprovedInPeriod(@Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    @Query("""
           SELECT SUM(s.rejected) 
           FROM SlaughterReport s 
           WHERE s.reportDate BETWEEN :startDate AND :endDate
           """)
    Integer getTotalRejectedInPeriod(@Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    @Query("""
           SELECT SUM(s.totalYield) 
           FROM SlaughterReport s 
           WHERE s.reportDate BETWEEN :startDate AND :endDate
           """)
    Float getTotalYieldInPeriod(@Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    @Query("""
           SELECT AVG(s.yieldPercentage) 
           FROM SlaughterReport s 
           WHERE s.reportDate BETWEEN :startDate AND :endDate
           """)
    Float getAverageYieldPercentage(@Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    @Query("""
           SELECT s 
           FROM SlaughterReport s 
           WHERE s.yieldPercentage < :threshold 
             AND s.reportDate BETWEEN :startDate AND :endDate
           """)
    List<SlaughterReport> findLowYieldReports(@Param("threshold") Float threshold,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);
}

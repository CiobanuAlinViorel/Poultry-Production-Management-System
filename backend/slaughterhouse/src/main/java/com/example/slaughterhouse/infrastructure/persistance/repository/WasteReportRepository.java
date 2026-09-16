package com.example.slaughterhouse.infrastructure.persistance.repository;

import com.example.slaughterhouse.domain.entities.WasteReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface WasteReportRepository extends JpaRepository<WasteReport, Long> {

    @Query("""
            SELECT w FROM WasteReport w
            WHERE w.reportPeriod.startDate >= :startDate
              AND w.reportPeriod.endDate <= :endDate
              AND w.isActive = true
            """)
    List<WasteReport> findAllWithinPeriod(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}

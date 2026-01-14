package com.example.slaughterhouse.infrastructure.persistance.repository;

import com.example.slaughterhouse.domain.entities.AnteMortemInspection;
import com.example.slaughterhouse.domain.entities.SlaughterLot;
import com.example.slaughterhouse.domain.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnteMortemInspectionRepository extends JpaRepository<AnteMortemInspection, Long> {

    Optional<AnteMortemInspection> findBySlaughterLot(SlaughterLot slaughterLot);

    List<AnteMortemInspection> findByInspectionDateBetween(LocalDate startDate, LocalDate endDate);

    List<AnteMortemInspection> findByApprovalStatus(ApprovalStatus approvalStatus);

    List<AnteMortemInspection> findByVeterinarianEmployeeId(Long veterinarianId);

    @Query("SELECT a FROM AnteMortemInspection a WHERE a.isActive = true")
    List<AnteMortemInspection> findAllActive();

    @Query("SELECT a FROM AnteMortemInspection a WHERE a.approvalStatus = :status AND a.inspectionDate BETWEEN :startDate AND :endDate")
    List<AnteMortemInspection> findByStatusAndDateRange(
            @Param("status") ApprovalStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT AVG(CAST(a.approved AS float) / a.totalInspected * 100) FROM AnteMortemInspection a WHERE a.inspectionDate BETWEEN :startDate AND :endDate")
    Float calculateAverageApprovalRate(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.ChicksReception;
import com.example.broilerfarm.domain.enums.ReceptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChicksReceptionRepository extends JpaRepository<ChicksReception, Long> {

    //Optional<ChicksReception> findByHatcheryDeliveryNoticeId(String noticeId);

    List<ChicksReception> findByFarmId(Long farmId);

    List<ChicksReception> findByStatus(ReceptionStatus status);

    @Query("SELECT cr FROM ChicksReception cr LEFT JOIN FETCH cr.receptionLines WHERE cr.id = :id")
    Optional<ChicksReception> findByIdWithLines(@Param("id") Long id);

    @Query("SELECT cr FROM ChicksReception cr WHERE cr.farm.id = :farmId " +
            "AND cr.receptionDate BETWEEN :startDate AND :endDate")
    List<ChicksReception> findByFarmIdAndDateRange(
            @Param("farmId") Long farmId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT cr FROM ChicksReception cr WHERE cr.status = 'DRAFT' " +
            "AND cr.receptionDate < :cutoffDate")
    List<ChicksReception> findOldDraftReceptions(@Param("cutoffDate") LocalDateTime cutoffDate);

    //boolean existsByHatcheryDeliveryNoticeId(String noticeId);
}
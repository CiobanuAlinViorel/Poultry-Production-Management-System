package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.ConsReceptionLine;
import com.example.broilerfarm.domain.enums.QualityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsReceptionLineRepository extends JpaRepository<ConsReceptionLine, Long> {

    List<ConsReceptionLine> findByReceptionId(Long receptionId);

    List<ConsReceptionLine> findByQualityStatus(QualityStatus status);

    @Query("SELECT crl FROM ConsReceptionLine crl WHERE crl.reception.id = :receptionId " +
            "AND crl.qualityStatus = :status")
    List<ConsReceptionLine> findByReceptionIdAndQualityStatus(
            @Param("receptionId") Long receptionId,
            @Param("status") QualityStatus status
    );

    @Query("SELECT crl FROM ConsReceptionLine crl WHERE crl.batchNumber = :batchNumber")
    List<ConsReceptionLine> findByBatchNumber(@Param("batchNumber") String batchNumber);
}
package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.TreatmentSheetLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TreatmentSheetLineRepository extends JpaRepository<TreatmentSheetLine, Long> {

    List<TreatmentSheetLine> findByTreatmentSheetId(Long treatmentSheetId);

    @Query("SELECT tsl FROM TreatmentSheetLine tsl WHERE tsl.medication.id = :medicationId")
    List<TreatmentSheetLine> findByMedicationId(@Param("medicationId") Long medicationId);

    @Query("SELECT tsl FROM TreatmentSheetLine tsl WHERE tsl.treatmentSheet.lot.id = :lotId " +
            "AND tsl.startDate <= :date AND tsl.endDate >= :date")
    List<TreatmentSheetLine> findActiveTreatmentLinesForLotOnDate(
            @Param("lotId") Long lotId,
            @Param("date") LocalDate date
    );

    // ✅ Returnează toate liniile pentru lot, verificarea withdrawal se face în Java
    @Query("SELECT tsl FROM TreatmentSheetLine tsl " +
            "WHERE tsl.treatmentSheet.lot.id = :lotId " +
            "AND tsl.treatmentSheet.status IN ('ACTIVE', 'COMPLETED')")
    List<TreatmentSheetLine> findAllLinesForWithdrawalCheck(@Param("lotId") Long lotId);

    @Query("SELECT tsl FROM TreatmentSheetLine tsl WHERE tsl.batchNumber = :batchNumber")
    List<TreatmentSheetLine> findByBatchNumber(@Param("batchNumber") String batchNumber);
}
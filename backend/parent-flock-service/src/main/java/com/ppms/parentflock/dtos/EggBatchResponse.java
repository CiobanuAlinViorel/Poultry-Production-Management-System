package com.ppms.parentflock.dtos;

import com.ppms.parentflock.domain.EggBatch;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EggBatchResponse(
        UUID id,
        UUID flockId,
        String batchCode,
        LocalDate laidDate,
        int storageDays,
        String qualityGrade,
        int quantity,
        String reservedFor,
        EggBatch.EggBatchStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}

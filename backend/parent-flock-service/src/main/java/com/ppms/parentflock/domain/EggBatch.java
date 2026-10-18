package com.ppms.parentflock.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "egg_batch", schema = "pf")
@Getter
@Setter
public class EggBatch extends BaseEntity {

    @Column(name = "flock_id", nullable = false)
    private UUID flockId;

    @Column(name = "batch_code", nullable = false, unique = true, length = 100)
    private String batchCode;

    @Column(name = "laid_date", nullable = false)
    private LocalDate laidDate;

    @Column(name = "storage_days", nullable = false)
    private int storageDays;

    @Column(name = "quality_grade", length = 20)
    private String qualityGrade;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "reserved_for", length = 255)
    private String reservedFor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EggBatchStatus status = EggBatchStatus.IN_STORE;

    public enum EggBatchStatus {
        IN_STORE, RESERVED, DISPATCHED, DISCARDED
    }
}

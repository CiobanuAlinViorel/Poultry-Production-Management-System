package com.ppms.hatchery.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "chick_batch", schema = "hatchery")
public class ChickBatch extends BaseEntity {

    @Column(name = "batch_code", nullable = false, unique = true)
    private String batchCode;

    @Column(name = "hatchery_id", nullable = false)
    private UUID hatcheryId;

    @Column(name = "hatch_record_id")
    private UUID hatchRecordId;

    @Column(name = "hatch_date")
    private Instant hatchDate;

    @Column(name = "grade")
    private String grade;

    @Column(name = "qty_available")
    private Integer qtyAvailable;

    @Column(name = "qty_boxed")
    private Integer qtyBoxed;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.AVAILABLE;

    public enum Status { AVAILABLE, RESERVED, DISPATCHED, CULLED }
}

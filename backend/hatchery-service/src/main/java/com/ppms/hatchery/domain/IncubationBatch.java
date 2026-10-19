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
@Table(name = "incubation_batch", schema = "ha")
public class IncubationBatch extends BaseEntity {

    @Column(name = "batch_code", nullable = false, unique = true)
    private String batchCode;

    @Column(name = "hatchery_id", nullable = false)
    private UUID hatcheryId;

    @Column(name = "setter_id")
    private UUID setterId;

    @Column(name = "hatcher_id")
    private UUID hatcherId;

    @Column(name = "set_at")
    private Instant setAt;

    @Column(name = "expected_hatch_date")
    private Instant expectedHatchDate;

    @Column(name = "qty_set")
    private Integer qtySet;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.SET;

    public enum Status { SET, CANDLED, TRANSFERRED, HATCHED, CLOSED, ABORTED }
}

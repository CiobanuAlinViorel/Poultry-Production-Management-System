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
@Table(name = "egg_batch", schema = "hatchery")
public class EggBatch extends BaseEntity {

    @Column(name = "batch_code", nullable = false, unique = true)
    private String batchCode;

    @Column(name = "hatchery_id", nullable = false)
    private UUID hatcheryId;

    @Column(name = "origin_flock_id")
    private UUID originFlockId;

    @Column(name = "origin_site_id")
    private UUID originSiteId;

    @Column(name = "breed_code")
    private String breedCode;

    @Column(name = "collection_date")
    private Instant collectionDate;

    @Column(name = "qty_accepted")
    private Integer qtyAccepted;

    @Column(name = "qty_available")
    private Integer qtyAvailable;

    @Column(name = "qty_set")
    private Integer qtySet;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.IN_STORE;

    public enum Status { IN_STORE, PARTIALLY_SET, FULLY_SET, DISCARDED }
}

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
@Table(name = "egg_transfer_order", schema = "pf")
@Getter
@Setter
public class EggTransferOrder extends BaseEntity {

    @Column(name = "from_site_id", nullable = false)
    private UUID fromSiteId;

    @Column(name = "to_hatchery_id", nullable = false)
    private UUID toHatcheryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status = TransferStatus.PLANNED;

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    public enum TransferStatus {
        PLANNED, APPROVED, DISPATCHED, RECEIVED, REJECTED, CANCELLED
    }
}

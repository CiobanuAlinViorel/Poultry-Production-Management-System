package com.ppms.farm.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "farm_delivery", schema = "fa")
@Getter @Setter @NoArgsConstructor
public class FarmDelivery extends BaseEntity {
    @Column(name = "lot_id", nullable = false) private UUID lotId;
    @Column(name = "dispatch_date", nullable = false) private LocalDate dispatchDate;
    @Column(nullable = false) private int quantity;
    @Column(name = "avg_weight_kg", nullable = false, precision = 10, scale = 3) private BigDecimal avgWeightKg;
    @Column(name = "fci_ref") private String fciRef;
}

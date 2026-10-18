package com.ppms.farm.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "silo", schema = "fa", uniqueConstraints = @UniqueConstraint(columnNames = {"farm_id", "code"}))
@Getter @Setter @NoArgsConstructor
public class Silo extends BaseEntity {
    @Column(name = "farm_id", nullable = false) private UUID farmId;
    @Column(nullable = false, length = 50) private String code;
    @Column(nullable = false) private String name;
    @Column(name = "capacity_kg", nullable = false, precision = 14, scale = 2) private BigDecimal capacityKg;
}

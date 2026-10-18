package com.ppms.farm.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "house", schema = "fa", uniqueConstraints = @UniqueConstraint(columnNames = {"farm_id", "code"}))
@Getter @Setter @NoArgsConstructor
public class House extends BaseEntity {
    @Column(name = "farm_id", nullable = false) private UUID farmId;
    @Column(nullable = false, length = 50) private String code;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private int capacity;
    @Column(name = "area_m2", nullable = false, precision = 12, scale = 2) private BigDecimal areaM2;
    @Column(nullable = false) private boolean active = true;
}

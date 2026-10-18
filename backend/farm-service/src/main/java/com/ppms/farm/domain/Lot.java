package com.ppms.farm.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "lot", schema = "fa")
@Getter @Setter @NoArgsConstructor
public class Lot extends BaseEntity {
    public enum Status { ACTIVE, CLOSED }
    @Column(name = "farm_id", nullable = false) private UUID farmId;
    @Column(name = "house_id", nullable = false) private UUID houseId;
    @Column(name = "breed_code", nullable = false, length = 100) private String breedCode;
    @Column(name = "chick_batch_id") private UUID chickBatchId;
    @Column(name = "arrival_date", nullable = false) private LocalDate arrivalDate;
    @Column(name = "female_count", nullable = false) private int femaleCount;
    @Column(name = "male_count", nullable = false) private int maleCount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status = Status.ACTIVE;
    @Column(name = "expected_catch_date") private LocalDate expectedCatchDate;
}

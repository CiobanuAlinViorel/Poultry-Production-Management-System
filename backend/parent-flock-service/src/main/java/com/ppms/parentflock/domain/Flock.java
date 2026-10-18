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
@Table(name = "flock", schema = "pf")
@Getter
@Setter
public class Flock extends BaseEntity {

    @Column(name = "site_id", nullable = false)
    private UUID siteId;

    @Column(name = "house_id", nullable = false)
    private UUID houseId;

    @Column(name = "breed_code", nullable = false, length = 50)
    private String breedCode;

    @Column(name = "arrival_date", nullable = false)
    private LocalDate arrivalDate;

    @Column(name = "expected_end_date")
    private LocalDate expectedEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FlockStatus status = FlockStatus.ACTIVE;

    @Column(name = "female_count", nullable = false)
    private int femaleCount;

    @Column(name = "male_count", nullable = false)
    private int maleCount;

    public enum FlockStatus {
        ACTIVE, CLOSED
    }
}

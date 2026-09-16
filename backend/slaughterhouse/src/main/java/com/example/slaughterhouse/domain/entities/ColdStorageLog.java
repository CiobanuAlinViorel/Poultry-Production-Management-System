package com.example.slaughterhouse.domain.entities;

import com.example.shared.domain.entity.BaseEntity;

import com.example.slaughterhouse.domain.enums.StorageStatus;
import com.example.slaughterhouse.domain.valueobjects.Temperature;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Represents a log entry for packages in cold storage
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "cold_storage_logs")
public class ColdStorageLog extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "package_id", nullable = false)
    private Package package_;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "entry_time", nullable = false)
    private LocalDateTime entryTime;

    @Column(name = "exit_date")
    private LocalDate exitDate;

    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    @Column(name = "storage_location", length = 100)
    private String storageLocation; // e.g., "Rack A-12", "Zone B-5"

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "temperature_value")),
            @AttributeOverride(name = "unit", column = @Column(name = "temperature_unit"))
    })
    private Temperature temperature;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private StorageStatus status = StorageStatus.STORED;

    @Column(name = "notes", length = 1000)
    private String notes;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private SlaughterhouseUser createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private SlaughterhouseUser updatedBy;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Version
    @Column(name = "version")
    private Integer version;


    // Business methods
    public Long calculateStorageDuration() {
        LocalDateTime end = exitTime != null ? exitTime : LocalDateTime.now();
        return ChronoUnit.DAYS.between(entryTime, end);
    }

    public Boolean isStillStored() {
        return exitDate == null && status == StorageStatus.STORED;
    }

    public void logExit() {
        this.exitDate = LocalDate.now();
        this.exitTime = LocalDateTime.now();
        this.status = StorageStatus.REMOVED;
    }
}
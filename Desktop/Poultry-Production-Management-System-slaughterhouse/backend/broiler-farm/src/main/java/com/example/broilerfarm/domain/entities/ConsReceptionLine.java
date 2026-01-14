package com.example.broilerfarm.domain.entities;

import com.example.broilerfarm.domain.enums.ApprovalStatus;
import com.example.broilerfarm.domain.enums.QualityStatus;
import com.example.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cons_reception_line")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsReceptionLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reception_id", nullable = false)
    private ConsumableReception reception;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumable_id", nullable = false)
    private Consumable consumable;

    @Column(name = "quantity_received", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityReceived;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "batch_number", nullable = false)
    private String batchNumber;

    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "storage_location")
    private String storageLocation;

    @Column(name = "quality_status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private QualityStatus qualityStatus = QualityStatus.ACCEPTED;

    // ✅ Calculated field
    @Transient
    public BigDecimal getLineTotal() {
        return quantityReceived.multiply(unitPrice);
    }

    // ✅ Business logic for validation
    public void validateDates() {
        if (expirationDate != null && manufacturingDate != null) {
            if (expirationDate.isBefore(manufacturingDate)) {
                throw new IllegalStateException(
                        "Expiration date cannot be before manufacturing date"
                );
            }
        }

        if (expirationDate != null && expirationDate.isBefore(LocalDate.now())) {
            throw new IllegalStateException(
                    "Cannot receive expired consumables. Expiration: " + expirationDate
            );
        }
    }

    // ✅ Quality inspection
    public void acceptItem() {
        if (this.reception.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Cannot change quality status after approval");
        }
        this.qualityStatus = QualityStatus.ACCEPTED;
    }

    public void rejectItem(String reason) {
        if (this.reception.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Cannot change quality status after approval");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        this.qualityStatus = QualityStatus.REJECTED;
        this.storageLocation = "REJECTED: " + reason;
    }

    public void quarantineItem(String reason) {
        if (this.reception.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Cannot change quality status after approval");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Quarantine reason is required");
        }
        this.qualityStatus = QualityStatus.QUARANTINE;
        this.storageLocation = "QUARANTINE: " + reason;
    }

    @Transient
    public boolean isAccepted() {
        return this.qualityStatus == QualityStatus.ACCEPTED;
    }

    @Transient
    public boolean isExpired() {
        return expirationDate != null && LocalDate.now().isAfter(expirationDate);
    }

    @Transient
    public boolean isExpiringSoon(int daysThreshold) {
        if (expirationDate == null) {
            return false;
        }
        LocalDate thresholdDate = LocalDate.now().plusDays(daysThreshold);
        return expirationDate.isBefore(thresholdDate) || expirationDate.isEqual(thresholdDate);
    }

    public ConsReceptionLine(Long id, LocalDateTime createdAt, LocalDateTime updatedAt, ConsumableReception reception, Consumable consumable, BigDecimal quantityReceived, BigDecimal unitPrice, String batchNumber, LocalDate manufacturingDate, LocalDate expirationDate, String storageLocation, QualityStatus qualityStatus) {
        super(id, createdAt, updatedAt);
        this.reception = reception;
        this.consumable = consumable;
        this.quantityReceived = quantityReceived;
        this.unitPrice = unitPrice;
        this.batchNumber = batchNumber;
        this.manufacturingDate = manufacturingDate;
        this.expirationDate = expirationDate;
        this.storageLocation = storageLocation;
        this.qualityStatus = qualityStatus;
    }
}
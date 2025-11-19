package com.example.broilerfarm.domain.entities;

import com.example.broilerfarm.domain.enums.ApprovalStatus;
import com.example.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "consumable_reception")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsumableReception extends BaseEntity {

    @Column(name = "supplier", nullable = false)
    private String supplier;

    @Column(name = "purchase_order_ref")
    private String purchaseOrderRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiving_warehouse_id", nullable = false)
    private Warehouse receivingWarehouse;

    @Column(name = "reception_date", nullable = false)
    @Builder.Default
    private LocalDate receptionDate = LocalDate.now();

    @Column(name = "total_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiving_employee_id", nullable = false)
    private FarmEmployee receivingEmployee;

    @Column(name = "approval_status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "notes", length = 1000)
    private String notes;

    // ✅ AGGREGATE: ConsumableReception contains its lines
    @OneToMany(mappedBy = "reception", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ConsReceptionLine> receptionLines = new ArrayList<>();

    // ✅ Business logic for lines management
    public void addReceptionLine(ConsReceptionLine line) {
        if (line == null) {
            throw new IllegalArgumentException("Reception line cannot be null");
        }

        if (this.approvalStatus != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Cannot add lines to an approved/rejected reception");
        }

        receptionLines.add(line);
        line.setReception(this);
        recalculateTotalAmount();
    }

    public void removeReceptionLine(ConsReceptionLine line) {
        if (this.approvalStatus != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Cannot remove lines from an approved/rejected reception");
        }

        receptionLines.remove(line);
        line.setReception(null);
        recalculateTotalAmount();
    }

    // ✅ Recalculates automatic the total sum
    private void recalculateTotalAmount() {
        this.totalAmount = receptionLines.stream()
                .map(ConsReceptionLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ✅ Approve status - CRITICAL: update stocks
    public void approve() {
        if (this.approvalStatus != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Reception already processed");
        }

        if (receptionLines.isEmpty()) {
            throw new IllegalStateException("Cannot approve reception without lines");
        }

        // Validation: all lines need to have quality status
        boolean allLinesInspected = receptionLines.stream()
                .allMatch(line -> line.getQualityStatus() != null);

        if (!allLinesInspected) {
            throw new IllegalStateException("All reception lines must be quality inspected");
        }

        this.approvalStatus = ApprovalStatus.APPROVED;

        // ✅ IMPORTANT: The updates for stocks are in Domain Service
        // Here you change just the status
    }

    public void reject(String reason) {
        if (this.approvalStatus != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Reception already processed");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        this.approvalStatus = ApprovalStatus.REJECTED;
        this.notes = (this.notes == null ? "" : this.notes + " | ") + "REJECTED: " + reason;
    }

    // ✅ Validare if is completed
    @Transient
    public boolean validateQuantities() {
        if (receptionLines.isEmpty()) {
            return false;
        }

        // Verify if PO exists and if the quantities are alright (optional)
        if (purchaseOrderRef != null) {
            // The validation logic will be in domain service
            return true;
        }

        return true;
    }

    @Transient
    public long getAcceptedLinesCount() {
        return receptionLines.stream()
                .filter(ConsReceptionLine::isAccepted)
                .count();
    }

    @Transient
    public long getRejectedLinesCount() {
        return receptionLines.stream()
                .filter(line -> !line.isAccepted())
                .count();
    }

    @Transient
    public BigDecimal getAcceptedAmount() {
        return receptionLines.stream()
                .filter(ConsReceptionLine::isAccepted)
                .map(ConsReceptionLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transient
    public boolean hasRejectedItems() {
        return receptionLines.stream()
                .anyMatch(line -> !line.isAccepted());
    }

    public ConsumableReception(Long id, LocalDateTime createdAt, LocalDateTime updatedAt, String purchaseOrderRef, String supplier, Warehouse receivingWarehouse, LocalDate receptionDate, BigDecimal totalAmount, FarmEmployee receivingEmployee, ApprovalStatus approvalStatus, String notes) {
        super(id, createdAt, updatedAt);
        this.purchaseOrderRef = purchaseOrderRef;
        this.supplier = supplier;
        this.receivingWarehouse = receivingWarehouse;
        this.receptionDate = receptionDate;
        this.totalAmount = totalAmount;
        this.receivingEmployee = receivingEmployee;
        this.approvalStatus = approvalStatus;
        this.notes = notes;
    }
}
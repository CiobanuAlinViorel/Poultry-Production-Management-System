package com.example.broilerfarm.domain.entities;

import com.example.broilerfarm.domain.enums.ApprovalStatus;
import com.example.broilerfarm.domain.enums.DataSource;
import com.example.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "delivery_notice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryNotice extends BaseEntity {

    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private BroilerFarm farm;

    @Column(name = "destination", nullable = false)
    private String destination; // Slaughterhouse name/code

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_manager_id", nullable = false)
    private FarmEmployee transportManager;

    @Column(name = "vehicle_info")
    private String vehicleInfo;

    @Column(name = "loading_time")
    private LocalDateTime loadingTime;

    @Column(name = "handling_requirements", length = 1000)
    private String handlingRequirements;

    @Column(name = "approval_status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private FarmEmployee approvedBy;

    @Column(name = "data_source", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DataSource dataSource = DataSource.OBSERVATION_SHEET;

    @Column(name = "slaughterhouse_dock")
    private String slaughterhouseDock; // Assigned by slaughterhouse

    @Column(name = "transmission_timestamp")
    private LocalDateTime transmissionTimestamp;

    @Column(name = "special_instructions", length = 1000)
    private String specialInstructions;

    // ✅ AGGREGATE: DeliveryNotice conține liniile sale
    @OneToMany(mappedBy = "deliveryNotice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DeliveryNoticeLine> deliveryLines = new ArrayList<>();

    // ✅ Business logic pentru gestionarea liniilor
    public void addDeliveryLine(DeliveryNoticeLine line) {
        if (line == null) {
            throw new IllegalArgumentException("Delivery line cannot be null");
        }

        if (this.approvalStatus != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Cannot add lines to a non-pending delivery notice");
        }

        deliveryLines.add(line);
        line.setDeliveryNotice(this);
    }

    public void removeDeliveryLine(DeliveryNoticeLine line) {
        if (this.approvalStatus != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Cannot remove lines from a non-pending delivery notice");
        }

        deliveryLines.remove(line);
        line.setDeliveryNotice(null);
    }

    // ✅ Aprobare (Farm Manager)
    public void approve(FarmEmployee approver) {
        if (this.approvalStatus != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Can only approve pending delivery notices");
        }

        if (deliveryLines.isEmpty()) {
            throw new IllegalStateException("Cannot approve delivery notice without lines");
        }

        validateAllLinesReady();

        this.approvalStatus = ApprovalStatus.APPROVED;
        this.approvedBy = approver;

        // Update lot statuses to READY_FOR_DELIVERY
        deliveryLines.forEach(line ->
                line.getLot().markAsReadyForDelivery()
        );
    }

    // ✅ Respingere
    public void reject(String reason) {
        if (this.approvalStatus != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Can only reject pending delivery notices");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        this.approvalStatus = ApprovalStatus.REJECTED;
        this.specialInstructions = (this.specialInstructions == null ? "" : this.specialInstructions + " | ")
                + "REJECTED: " + reason;
    }

    // ✅ Mark as sent to slaughterhouse (UC-11)
    public void markAsSent(String assignedDock, LocalDateTime confirmedSlot) {
        if (this.approvalStatus != ApprovalStatus.APPROVED) {
            throw new IllegalStateException("Can only send approved delivery notices");
        }

        this.transmissionTimestamp = LocalDateTime.now();
        this.slaughterhouseDock = assignedDock;

        if (confirmedSlot != null) {
            this.scheduledDate = confirmedSlot;
        }
    }

    // ✅ Mark as in transit
    public void markAsInTransit() {
        if (this.approvalStatus != ApprovalStatus.APPROVED) {
            throw new IllegalStateException("Only approved notices can be in transit");
        }

        // Update lot statuses to DELIVERED (or IN_TRANSIT if you have that status)
        deliveryLines.forEach(line -> {
            if (line.getLot().getStatus() == com.example.broilerfarm.domain.enums.ChicksLotStatus.READY_FOR_DELIVERY) {
                // Will be marked as DELIVERED when confirmed by slaughterhouse
            }
        });
    }

    // ✅ Validări
    private void validateAllLinesReady() {
        // Check all lots are eligible
        boolean allReady = deliveryLines.stream()
                .allMatch(line -> {
                    ChicksLot lot = line.getLot();
                    // Minimum age check
                    if (lot.getDaysInFarm() < 35) {
                        throw new IllegalStateException(
                                "Lot " + lot.getLotNumber() + " is below minimum age (35 days)"
                        );
                    }
                    return true;
                });

        if (!allReady) {
            throw new IllegalStateException("Not all lots are ready for delivery");
        }
    }

    // ✅ Calculated fields
    @Transient
    public Integer getTotalEstimatedQuantity() {
        return deliveryLines.stream()
                .mapToInt(DeliveryNoticeLine::getEstimatedQuantity)
                .sum();
    }

    @Transient
    public BigDecimal getTotalEstimatedWeight() {
        return deliveryLines.stream()
                .map(DeliveryNoticeLine::getTotalEstimatedWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transient
    public BigDecimal getOverallAverageWeight() {
        int totalQuantity = getTotalEstimatedQuantity();
        if (totalQuantity == 0) {
            return BigDecimal.ZERO;
        }

        return getTotalEstimatedWeight()
                .divide(BigDecimal.valueOf(totalQuantity), 3, java.math.RoundingMode.HALF_UP);
    }

    @Transient
    public boolean isReadyToSend() {
        return this.approvalStatus == ApprovalStatus.APPROVED &&
                this.transmissionTimestamp == null;
    }

    @Transient
    public boolean isSent() {
        return this.transmissionTimestamp != null;
    }

    @Transient
    public boolean hasMultipleLots() {
        return deliveryLines.size() > 1;
    }
}
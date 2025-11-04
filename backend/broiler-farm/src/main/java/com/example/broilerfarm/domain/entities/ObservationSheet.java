package com.example.broilerfarm.domain.entities;

import com.example.broilerfarm.domain.enums.ObservationSheetStatus;
import com.example.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "observation_sheet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObservationSheet extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private ChicksLot lot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "observer_id", nullable = false)
    private FarmEmployee observer;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ObservationSheetStatus status = ObservationSheetStatus.DRAFT;

    // ✅ AUTO-CALCULATED metrics (from MortalitySheet & ConsumptionSheet)
    @Column(name = "starting_bird_count", nullable = false)
    private Integer startingBirdCount;

    @Column(name = "ending_bird_count", nullable = false)
    private Integer endingBirdCount;

    @Column(name = "total_mortality", nullable = false)
    @Builder.Default
    private Integer totalMortality = 0;

    @Column(name = "average_daily_mortality", precision = 10, scale = 2)
    private BigDecimal averageDailyMortality;

    @Column(name = "total_feed_consumed", nullable = false)
    @Builder.Default
    private Integer totalFeedConsumed = 0;

    @Column(name = "total_water_consumed", nullable = false)
    @Builder.Default
    private Integer totalWaterConsumed = 0;

    // ✅ MANUAL INPUT - Weight sampling
    @Column(name = "sample_size", nullable = false)
    private Integer sampleSize;

    @Column(name = "average_weight", nullable = false, precision = 10, scale = 3)
    private BigDecimal averageWeight;

    @Column(name = "weight_std_dev", precision = 10, scale = 3)
    private BigDecimal weightStdDev;

    @Column(name = "max_weight", precision = 10, scale = 3)
    private BigDecimal maxWeight;

    @Column(name = "min_weight", precision = 10, scale = 3)
    private BigDecimal minWeight;

    // ✅ CALCULATED performance metrics
    @Column(name = "weight_gain", precision = 10, scale = 3)
    private BigDecimal weightGain;

    @Column(name = "fcr", precision = 10, scale = 4)
    private BigDecimal fcr; // Feed Conversion Ratio

    @Column(name = "adg", precision = 10, scale = 3)
    private BigDecimal adg; // Average Daily Gain

    // ✅ MANUAL INPUT - Observations
    @Column(name = "health_observations", length = 1000)
    private String healthObservations;

    @Column(name = "behavioral_notes", length = 1000)
    private String behavioralNotes;

    @Column(name = "environmental_notes", length = 1000)
    private String environmentalNotes;

    @Column(name = "concerns", length = 1000)
    private String concerns;

    // ✅ Business logic - Auto-populate from data sources
    public void autoPopulateFromDataSources(
            Integer previousWeekBirdCount,
            Integer currentBirdCount,
            Integer weeklyMortality,
            Integer weeklyFeedConsumed,
            Integer weeklyWaterConsumed) {

        this.startingBirdCount = previousWeekBirdCount;
        this.endingBirdCount = currentBirdCount;
        this.totalMortality = weeklyMortality;
        this.totalFeedConsumed = weeklyFeedConsumed;
        this.totalWaterConsumed = weeklyWaterConsumed;

        // Calculate average daily mortality
        if (weeklyMortality != null) {
            this.averageDailyMortality = BigDecimal.valueOf(weeklyMortality)
                    .divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);
        }
    }

    // ✅ Calculate performance metrics after weight input
    public void calculatePerformanceMetrics(BigDecimal previousWeekAverageWeight) {
        if (this.status != ObservationSheetStatus.DRAFT) {
            throw new IllegalStateException("Cannot recalculate metrics for non-draft sheets");
        }

        // Weight gain (current week - previous week)
        if (previousWeekAverageWeight != null && this.averageWeight != null) {
            this.weightGain = this.averageWeight.subtract(previousWeekAverageWeight);
        }

        // FCR = Total Feed Consumed / Total Weight Gain
        if (this.weightGain != null && this.weightGain.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalWeightGain = this.weightGain.multiply(
                    BigDecimal.valueOf(this.endingBirdCount)
            );

            this.fcr = BigDecimal.valueOf(this.totalFeedConsumed)
                    .divide(totalWeightGain, 4, RoundingMode.HALF_UP);
        }

        // ADG = Weight Gain / 7 days
        if (this.weightGain != null) {
            this.adg = this.weightGain
                    .divide(BigDecimal.valueOf(7), 3, RoundingMode.HALF_UP);
        }
    }

    // ✅ Submit for approval
    public void submit() {
        if (this.status != ObservationSheetStatus.DRAFT) {
            throw new IllegalStateException("Can only submit draft observation sheets");
        }

        validateCompleteness();

        this.status = ObservationSheetStatus.SUBMITTED;
    }

    // ✅ Approve (Farm Manager)
    public void approve() {
        if (this.status != ObservationSheetStatus.SUBMITTED) {
            throw new IllegalStateException("Can only approve submitted sheets");
        }

        this.status = ObservationSheetStatus.APPROVED;

        // ✅ After approval, check if lot is ready for delivery
        // This logic happens in Domain Service (UC-05 decision point)
    }

    // ✅ Reject
    public void reject(String reason) {
        if (this.status != ObservationSheetStatus.SUBMITTED) {
            throw new IllegalStateException("Can only reject submitted sheets");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        this.status = ObservationSheetStatus.REJECTED;
        this.concerns = (this.concerns == null ? "" : this.concerns + " | ")
                + "REJECTED: " + reason;
    }

    // ✅ Validations
    private void validateCompleteness() {
        if (sampleSize == null || sampleSize < 10) {
            throw new IllegalStateException("Minimum 10 birds required for weight sampling");
        }

        if (averageWeight == null) {
            throw new IllegalStateException("Average weight is required");
        }

        if (healthObservations == null || healthObservations.trim().isEmpty()) {
            throw new IllegalStateException("Health observations are required");
        }
    }

    // ✅ Check if lot is ready for delivery (UC-05 decision point)
    @Transient
    public boolean isLotEligibleForDelivery(int minimumAge) {
        // Lot age >= minimum days (typically 35)
        int lotAge = this.lot.getDaysInFarm();
        if (lotAge < minimumAge) {
            return false;
        }

        // No active withdrawal periods (checked in Service)
        // Status must be APPROVED
        return this.status == ObservationSheetStatus.APPROVED;
    }

    // ✅ Performance alerts
    @Transient
    public boolean isFcrOutOfRange(BigDecimal maxAcceptableFcr) {
        return this.fcr != null && this.fcr.compareTo(maxAcceptableFcr) > 0;
    }

    @Transient
    public boolean isMortalityHigh(BigDecimal expectedRate, BigDecimal tolerance) {
        if (this.startingBirdCount == 0) {
            return false;
        }

        BigDecimal actualRate = BigDecimal.valueOf(this.totalMortality)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(this.startingBirdCount), 2, RoundingMode.HALF_UP);

        BigDecimal threshold = expectedRate.add(tolerance);
        return actualRate.compareTo(threshold) > 0;
    }

    @Transient
    public boolean isWeightGainBelowExpected(BigDecimal expectedWeightGain) {
        return this.weightGain != null &&
                this.weightGain.compareTo(expectedWeightGain) < 0;
    }

    @Transient
    public boolean requiresInvestigation(BigDecimal maxFcr,
                                         BigDecimal expectedMortalityRate,
                                         BigDecimal expectedWeightGain) {
        return isFcrOutOfRange(maxFcr) ||
                isMortalityHigh(expectedMortalityRate, BigDecimal.valueOf(2.0)) ||
                isWeightGainBelowExpected(expectedWeightGain);
    }

    // ✅ Mortality rate calculation
    @Transient
    public BigDecimal getMortalityRate() {
        if (startingBirdCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(totalMortality)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(startingBirdCount), 2, RoundingMode.HALF_UP);
    }

    // ✅ Weight variance check
    @Transient
    public boolean hasHighWeightVariance(BigDecimal maxVariancePercentage) {
        if (weightStdDev == null || averageWeight == null) {
            return false;
        }

        BigDecimal coefficientOfVariation = weightStdDev
                .divide(averageWeight, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return coefficientOfVariation.compareTo(maxVariancePercentage) > 0;
    }
}
package com.example.broilerfarm.application.dto;
import com.example.broilerfarm.domain.enums.ObservationSheetStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObservationSheetDto {
    private Long id;
    private String lotNumber;
    private Long observer;
    private Integer weekNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private ObservationSheetStatus status;
    private Integer endingBirdCount;
    private Integer startingBirdCount;
    private Integer totalMortality;
    private BigDecimal averageDailyMortality;
    private Integer totalFeedConsumed;
    private Integer totalWaterConsumed;
    private Integer sampleSize;
    private BigDecimal averageWeight;
    private BigDecimal weightStdDev;
    private BigDecimal maxWeight;
    private BigDecimal minWeight;
    private BigDecimal weightGain;
    private BigDecimal fcr;
    private BigDecimal adg;
    private String healthObservations;
    private String behavioralNotes;
    private String environmentalNotes;
    private String concerns;
}

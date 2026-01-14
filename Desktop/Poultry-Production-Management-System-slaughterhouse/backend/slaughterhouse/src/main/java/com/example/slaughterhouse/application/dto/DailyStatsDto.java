package com.example.slaughterhouse.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for DailyStats entity
 * Represents daily operational statistics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatsDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("stats_date")
    private LocalDate statsDate;

    @JsonProperty("total_chickens_received")
    private Integer totalChickensReceived;

    @JsonProperty("total_chickens_slaughtered")
    private Integer totalChickensSlaughtered;

    @JsonProperty("total_products_produced")
    private Integer totalProductsProduced;

    @JsonProperty("total_packages_created")
    private Integer totalPackagesCreated;

    @JsonProperty("total_weight_produced_value")
    private Double totalWeightProducedValue;

    @JsonProperty("total_weight_produced_unit")
    private String totalWeightProducedUnit;

    @JsonProperty("total_waste_value")
    private Double totalWasteValue;

    @JsonProperty("total_waste_unit")
    private String totalWasteUnit;

    @JsonProperty("efficiency_rate")
    private Float efficiencyRate;

    @JsonProperty("mortality_rate")
    private Float mortalityRate;

    @JsonProperty("rejection_rate")
    private Float rejectionRate;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("created_by_id")
    private Long createdById;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("updated_by_id")
    private Long updatedById;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("version")
    private Integer version;
}
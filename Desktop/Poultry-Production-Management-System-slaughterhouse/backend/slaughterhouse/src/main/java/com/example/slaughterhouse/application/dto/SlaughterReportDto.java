package com.example.slaughterhouse.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for SlaughterReport entity
 * Represents daily slaughter report
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlaughterReportDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("report_date")
    private LocalDate reportDate;

    @JsonProperty("slaughter_lot_id")
    private Long slaughterLotId;

    @JsonProperty("total_slaughtered")
    private Integer totalSlaughtered;

    @JsonProperty("total_weight_value")
    private Double totalWeightValue;

    @JsonProperty("total_weight_unit")
    private String totalWeightUnit;

    @JsonProperty("average_carcass_weight_value")
    private Double averageCarcassWeightValue;

    @JsonProperty("average_carcass_weight_unit")
    private String averageCarcassWeightUnit;

    @JsonProperty("products_produced")
    private Integer productsProduced;

    @JsonProperty("waste_generated_value")
    private Double wasteGeneratedValue;

    @JsonProperty("waste_generated_unit")
    private String wasteGeneratedUnit;

    @JsonProperty("efficiency_percentage")
    private Float efficiencyPercentage;

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
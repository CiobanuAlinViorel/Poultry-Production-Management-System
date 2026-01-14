package com.example.slaughterhouse.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for WasteReport entity
 * Represents waste generated during slaughter process
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WasteReportDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("report_date")
    private LocalDate reportDate;

    @JsonProperty("slaughter_lot_id")
    private Long slaughterLotId;

    @JsonProperty("waste_type")
    private String wasteType;

    @JsonProperty("quantity_value")
    private Double quantityValue;

    @JsonProperty("quantity_unit")
    private String quantityUnit;

    @JsonProperty("disposal_method")
    private String disposalMethod;

    @JsonProperty("disposal_cost")
    private Double disposalCost;

    @JsonProperty("responsible_employee_id")
    private Long responsibleEmployeeId;

    @JsonProperty("responsible_employee_name")
    private String responsibleEmployeeName;

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
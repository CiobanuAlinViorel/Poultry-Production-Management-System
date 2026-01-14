package com.example.slaughterhouse.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for PackagingSheet entity
 * Represents packaging sheet/batch
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackagingSheetDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("sheet_number")
    private String sheetNumber;

    @JsonProperty("packaging_date")
    private LocalDate packagingDate;

    @JsonProperty("slaughter_lot_id")
    private Long slaughterLotId;

    @JsonProperty("total_packages")
    private Integer totalPackages;

    @JsonProperty("total_weight_value")
    private Double totalWeightValue;

    @JsonProperty("total_weight_unit")
    private String totalWeightUnit;

    @JsonProperty("package_ids")
    private List<Long> packageIds;

    @JsonProperty("responsible_employee_id")
    private Long responsibleEmployeeId;

    @JsonProperty("responsible_employee_name")
    private String responsibleEmployeeName;

    @JsonProperty("status")
    private String status;

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
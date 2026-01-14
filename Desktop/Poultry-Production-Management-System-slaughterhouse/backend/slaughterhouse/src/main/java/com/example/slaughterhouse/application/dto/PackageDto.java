package com.example.slaughterhouse.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Package entity
 * Represents a package containing products
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("packaging_sheet_id")
    private Long packagingSheetId;

    @JsonProperty("product_ids")
    private List<Long> productIds;

    @JsonProperty("package_code_prefix")
    private String packageCodePrefix;

    @JsonProperty("package_code_number")
    private Integer packageCodeNumber;

    @JsonProperty("package_code_suffix")
    private String packageCodeSuffix;

    @JsonProperty("package_code_full")
    private String packageCodeFull;

    @JsonProperty("weight_value")
    private Double weightValue;

    @JsonProperty("weight_unit")
    private String weightUnit;

    @JsonProperty("packaging_date")
    private LocalDate packagingDate;

    @JsonProperty("expiry_date")
    private LocalDate expiryDate;

    @JsonProperty("package_type")
    private String packageType;

    @JsonProperty("status")
    private String status;

    @JsonProperty("cold_storage_log_id")
    private Long coldStorageLogId;

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
package com.example.slaughterhouse.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for ColdStorageLog entity
 * Represents cold storage log for packages
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColdStorageLogDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("package_id")
    private Long packageId;

    @JsonProperty("storage_location")
    private String storageLocation;

    @JsonProperty("entry_time")
    private LocalDateTime entryTime;

    @JsonProperty("exit_time")
    private LocalDateTime exitTime;

    @JsonProperty("temperature")
    private Float temperature;

    @JsonProperty("humidity")
    private Float humidity;

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
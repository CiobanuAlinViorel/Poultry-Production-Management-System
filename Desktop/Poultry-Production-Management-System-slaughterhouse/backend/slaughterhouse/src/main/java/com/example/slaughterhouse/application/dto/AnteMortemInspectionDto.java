package com.example.slaughterhouse.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnteMortemInspectionDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("slaughter_lot_id")
    private Long slaughterLotId;

    @JsonProperty("inspection_date")
    private LocalDate inspectionDate;

    @JsonProperty("veterinarian_id")
    private Long veterinarianId;

    @JsonProperty("veterinarian_name")
    private String veterinarianName;

    @JsonProperty("total_inspected")
    private Integer totalInspected;

    @JsonProperty("approved")
    private Integer approved;

    @JsonProperty("rejected")
    private Integer rejected;

    @JsonProperty("health_status")
    private String healthStatus;

    @JsonProperty("health_description")
    private String healthDescription;

    @JsonProperty("health_severity")
    private String healthSeverity;

    @JsonProperty("approval_status")
    private String approvalStatus;

    @JsonProperty("rejection_reasons")
    private String rejectionReasons;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("approval_rate")
    private Float approvalRate;

    @JsonProperty("rejection_rate")
    private Float rejectionRate;

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
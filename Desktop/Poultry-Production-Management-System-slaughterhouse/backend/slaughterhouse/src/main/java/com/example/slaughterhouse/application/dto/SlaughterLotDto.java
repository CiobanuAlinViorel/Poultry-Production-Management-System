package com.example.slaughterhouse.application.dto;

import com.example.slaughterhouse.domain.enums.LotStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("slaughter_lot")
public class SlaughterLotDto {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("lot_number")
    private String lotNumber;
    
    @JsonProperty("breed")
    private String breed;
    
    @JsonProperty("slaughter_date")
    private LocalDate slaughterDate;
    
    @JsonProperty("total_chickens")
    private Integer totalChickens;
    
    @JsonProperty("current_quantity")
    private Integer currentQuantity;
    
    @JsonProperty("average_age_in_days")
    private Integer averageAgeInDays;
    
    @JsonProperty("status")
    private LotStatus status;
    
    @JsonProperty("average_weight_value")
    private Float averageWeightValue;
    
    @JsonProperty("average_weight_unit")
    private String averageWeightUnit;
    
    @JsonProperty("total_weight_value")
    private Float totalWeightValue;
    
    @JsonProperty("total_weight_unit")
    private String totalWeightUnit;
    
    @JsonProperty("health_status")
    private String healthStatus;
    
    @JsonProperty("health_description")
    private String healthDescription;
    
    @JsonProperty("health_severity")
    private String healthSeverity;
    
    @JsonProperty("quality_grade")
    private String qualityGrade;
    
    @JsonProperty("quality_score")
    private Double qualityScore;
    
    @JsonProperty("quality_description")
    private String qualityDescription;
    
    @JsonProperty("manager_id")
    private Long managerId;
    
    @JsonProperty("is_active")
    private Boolean isActive;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    // Relations (optional - for full representation)
    @JsonProperty("products")
    private List<ProductDto> products;
}

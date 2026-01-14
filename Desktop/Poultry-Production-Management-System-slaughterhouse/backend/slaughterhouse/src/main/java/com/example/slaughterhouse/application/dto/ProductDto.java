package com.example.slaughterhouse.application.dto;

import com.example.slaughterhouse.domain.enums.ProductStatus;
import com.example.slaughterhouse.domain.enums.ProductType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("product")
public class ProductDto {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("slaughter_lot_id")
    private Long slaughterLotId;
    
    @JsonProperty("product_type")
    private ProductType productType;
    
    @JsonProperty("cut")
    private String cut;
    
    @JsonProperty("weight_value")
    private Double weightValue;
    
    @JsonProperty("weight_unit")
    private String weightUnit;
    
    @JsonProperty("quality_grade")
    private String qualityGrade;
    
    @JsonProperty("quality_score")
    private Double qualityScore;
    
    @JsonProperty("quality_description")
    private String qualityDescription;
    
    @JsonProperty("production_date")
    private LocalDate productionDate;
    
    @JsonProperty("status")
    private ProductStatus status;
    
    @JsonProperty("batch_number")
    private String batchNumber;
    
    @JsonProperty("inspection_passed")
    private Boolean inspectionPassed;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @JsonProperty("is_active")
    private Boolean isActive;
}

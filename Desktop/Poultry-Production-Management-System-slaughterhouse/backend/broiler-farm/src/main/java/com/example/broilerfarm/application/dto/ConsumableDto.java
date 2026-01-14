package com.example.broilerfarm.application.dto;

import com.example.broilerfarm.domain.enums.ConsumableType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsumableDto {
    private Long id;
    private String name;
    private ConsumableType consumableType;
    private String category;
    private String unitOfMeasure;
    private BigDecimal reorderPoint;
    private BigDecimal standardPrice;
    private String supplier;
    private String storageRequirements;
    private Integer shelfLife;
}

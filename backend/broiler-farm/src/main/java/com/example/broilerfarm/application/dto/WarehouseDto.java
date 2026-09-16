package com.example.broilerfarm.application.dto;

import com.example.broilerfarm.domain.entities.FarmEmployee;
import com.example.shared.domain.enums.WarehouseType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseDto {
    private Long id;
    private String warehouseName;
    private WarehouseType type;
    private BigDecimal capacity;
    private BigDecimal currentOccupancy;
    private Long farmId;
    private FarmEmployee responsibleEmployee;
}

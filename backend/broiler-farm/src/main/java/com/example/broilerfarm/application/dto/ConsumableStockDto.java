package com.example.broilerfarm.application.dto;
import com.example.broilerfarm.domain.enums.StockStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConsumableStockDto {
    private Long id;
    private Long consumableId;
    private Long warehouseId;
    private String batchNumber;
    private BigDecimal quantityOnHand;
    private BigDecimal reservedQuantity;
    private LocalDate manufacturingDate;
    private LocalDate lastRestockDate;
    private LocalDate expirationDate;
    private StockStatus status;
}


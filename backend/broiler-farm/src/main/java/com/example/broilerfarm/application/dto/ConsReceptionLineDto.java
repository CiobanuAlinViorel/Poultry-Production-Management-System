package com.example.broilerfarm.application.dto;
import com.example.broilerfarm.domain.enums.QualityStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsReceptionLineDto {
    private Long id;
    private Long receptionId;
    private Long consumableId;
    private BigDecimal quantityReceived;
    private BigDecimal unitPrice;
    private String batchNumber;
    private LocalDate manufacturingDate;
    private LocalDate expirationDate;
    private String storageLocation;
    private QualityStatus qualityStatus;
}

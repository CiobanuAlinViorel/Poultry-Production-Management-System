package com.example.broilerfarm.application.dto;
import com.example.broilerfarm.domain.enums.AdministrationMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TreatmentSheetLineDto {
    private Long id;
    private Long treatmentSheetId;
    private Long medication;
    private BigDecimal dosage;
    private String dosageUnit;
    private AdministrationMethod administrationMethod;
    private Integer duration;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer withdrawalPeriod;
    private BigDecimal quantityUsed;
    private String batchNumber;
}

package com.example.broilerfarm.services;

import com.example.broilerfarm.domain.entities.Consumable;
import com.example.broilerfarm.domain.entities.TreatmentSheet;
import com.example.broilerfarm.domain.enums.AdministrationMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ITreatmentSheetService {
    void addLine(Long treatmentSheetId,
                 Consumable medication,
                 BigDecimal dosage,
                 String dosageUnit,
                 AdministrationMethod administrationMethod,
                 Integer duration,
                 LocalDate startDate,
                 Integer withdrawalPeriod,
                 BigDecimal quantityUsed,
                 String batchNumber);
    void updateLine(Long id,
                    Consumable medication,
                    BigDecimal dosage,
                    String dosageUnit,
                    AdministrationMethod administrationMethod,
                    Integer duration,
                    LocalDate startDate,
                    Integer withdrawalPeriod,
                    BigDecimal quantityUsed,
                    String batchNumber);
    void deleteLine(Long id);
}

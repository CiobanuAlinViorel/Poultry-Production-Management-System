package com.example.broilerfarm.services;

import com.example.broilerfarm.domain.entities.Consumable;
import com.example.broilerfarm.domain.entities.TreatmentSheet;
import com.example.broilerfarm.domain.entities.TreatmentSheetLine;
import com.example.broilerfarm.domain.enums.AdministrationMethod;
import com.example.broilerfarm.infrastructure.persistence.repositories.ConsumableRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.TreatmentSheetLineRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.TreatmentSheetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Transactional
public class TreatmentSheetServiceImplementation implements ITreatmentSheetService{

    @Autowired
    private TreatmentSheetRepository treatmentSheetRepository;

    @Autowired
    private ConsumableRepository consumableRepository;

    @Autowired
    private TreatmentSheetLineRepository treatmentSheetLineRepository;


    @Override
    public void addLine(Long treatmentSheetId,
                        Consumable medication,
                        BigDecimal dosage,
                        String dosageUnit,
                        AdministrationMethod administrationMethod,
                        Integer duration,
                        LocalDate startDate,
                        Integer withdrawalPeriod,
                        BigDecimal quantityUsed,
                        String batchNumber) {
      if(treatmentSheetId == null){
          throw new IllegalArgumentException("TreatmentSheet is null");
      }

      verifyConsumable(medication, dosage, dosageUnit, administrationMethod, quantityUsed);
      verifyTimingAndRules(duration, startDate, withdrawalPeriod, batchNumber);

      TreatmentSheet sheet = treatmentSheetRepository.findById(treatmentSheetId).orElse(null);
        if(sheet == null){
            throw new IllegalArgumentException("TreatmentSheet not found");
        }
        TreatmentSheetLine line =  new TreatmentSheetLine(
                sheet,
                medication,
                dosage,
                dosageUnit,
                administrationMethod,
                duration,
                startDate,
                startDate.plusDays(duration),
                withdrawalPeriod,
                quantityUsed,
                batchNumber
        );
        sheet.addTreatmentLine(line);
        if(sheet.getTreatmentLines().isEmpty() || startDate.isBefore(sheet.getTreatmentDate())){
            sheet.setTreatmentDate(startDate);
        }
        treatmentSheetRepository.save(sheet);
    }

    @Override
    public void updateLine(Long id, Consumable medication, BigDecimal dosage, String dosageUnit, AdministrationMethod administrationMethod, Integer duration, LocalDate startDate, Integer withdrawalPeriod, BigDecimal quantityUsed, String batchNumber) {
       if(id == null){
           throw new IllegalArgumentException("Id is null");
       }

        verifyConsumable(medication, dosage, dosageUnit, administrationMethod, quantityUsed);
        verifyTimingAndRules(duration, startDate, withdrawalPeriod, batchNumber);

        TreatmentSheetLine line = treatmentSheetLineRepository.findById(id).orElse(null);

        if(line == null){
            throw new IllegalArgumentException("TreatmentSheet not found");
        }

        line.setMedication(medication);
        line.setDosage(dosage);
        line.setDosageUnit(dosageUnit);
        line.setAdministrationMethod(administrationMethod);
        line.setDuration(duration);
        line.setStartDate(startDate);
        line.setEndDate(startDate.plusDays(duration));
        line.setWithdrawalPeriod(withdrawalPeriod);
        line.setQuantityUsed(quantityUsed);
        line.setBatchNumber(batchNumber);

        if(line.getTreatmentSheet().getTreatmentDate().isBefore(startDate)){
            line.getTreatmentSheet().setTreatmentDate(startDate);
        }

        treatmentSheetRepository.save(line.getTreatmentSheet());

    }

    @Override
    public void deleteLine(Long id) {
       if(id == null){
            throw new IllegalArgumentException("Id is null");
       }

       TreatmentSheetLine line = treatmentSheetLineRepository.findById(id).orElse(null);
       if(line == null){
           throw new IllegalArgumentException("TreatmentSheet not found");
       }
       TreatmentSheet sheet = line.getTreatmentSheet();
       sheet.removeTreatmentLine(line);
       treatmentSheetLineRepository.delete(line);
       if(sheet.getTreatmentLines().isEmpty()){
           sheet.setTreatmentDate(null);
       }else{
           LocalDate minimalDate = sheet.getTreatmentLines().getFirst().getStartDate();
           for(TreatmentSheetLine l: sheet.getTreatmentLines()){
               if(l.getStartDate().isBefore(minimalDate)){
                   minimalDate = l.getStartDate();
               }
           }
           sheet.setTreatmentDate(minimalDate);
       }
       treatmentSheetRepository.save(sheet);
    }


    private void verifyConsumable(Consumable medication,
                                  BigDecimal dosage,
                                  String dosageUnit,
                                  AdministrationMethod administrationMethod,
                                  BigDecimal quantityUsed){
        if(medication == null){
            throw new IllegalArgumentException("medication cannot be null");
        }

        if(dosage.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Dosage cannot be negative or 0");
        }

        if(dosageUnit.isEmpty()){
            throw new IllegalArgumentException("DosageUnit cannot be null or empty");
        }

        if(administrationMethod == null){
            throw new IllegalArgumentException("AdministrationMethod cannot be null");
        }

        if(quantityUsed.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Quantity used cannot be negative or 0");
        }
    }

    private void verifyTimingAndRules( Integer duration,
                               LocalDate startDate,
                               Integer withdrawalPeriod,
                               String batchNumber){
        if(duration <= 0){
            throw new IllegalArgumentException("Duration cannot be negative or 0");
        }
        if(startDate == null ||  startDate.isBefore(LocalDate.now())){
            throw new IllegalArgumentException("Start date cannot be null or before today");
        }

        if(withdrawalPeriod <= 0){
            throw new IllegalArgumentException("Withdrawal period cannot be negative or 0");
        }

        if(batchNumber == null || batchNumber.isEmpty()){
            throw new IllegalArgumentException("Batch number cannot be null or empty");
        }
    }
}

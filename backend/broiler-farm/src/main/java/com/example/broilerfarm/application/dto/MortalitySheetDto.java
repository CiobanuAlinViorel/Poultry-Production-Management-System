package com.example.broilerfarm.application.dto;
import com.example.broilerfarm.domain.enums.DisposalMethod;
import com.example.broilerfarm.domain.enums.MortalitySheetStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MortalitySheetDto {
    private Long id;
    private String lotNumber;
    private Long reportingEmployeeId;
    private LocalDate sheetDate;
    private Integer totalMortality;
    private Integer cumulativeMortality;
    private MortalitySheetStatus status;
    private String primaryCause;
    private Integer ageInDays;
    private BigDecimal averageWeight;
    private DisposalMethod disposalMethod;
    private String locationNotes;
    private String observations;
}

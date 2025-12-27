package com.example.broilerfarm.application.dto;
import com.example.broilerfarm.domain.enums.ConsumptionSheetStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ConsumptionSheetDto {
    @EqualsAndHashCode.Include
    private Long id;
    private String lotNumber;
    private LocalDate sheetDate;
    private Integer birdsAlive;
    private ConsumptionSheetStatus status;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<ConsumptionSheetLineDto> consumptionSheetLines = new ArrayList<>();

    public void addLine(ConsumptionSheetLineDto lineDto){
        consumptionSheetLines.add(lineDto);
    }

    public void deleteLine(Long lineId){
        consumptionSheetLines.removeIf(l -> l.getId().equals(lineId));
    }
}

package com.example.broilerfarm.application.dto;
import com.example.broilerfarm.domain.enums.TreatmentStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TreatmentSheetDto {
    @EqualsAndHashCode.Include
    private Long id;
    private Long lotId;
    private Long veterinarian;
    private String diagnosis;
    private String treatmentReason;
    private LocalDate treatmentDate;
    private TreatmentStatus status;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<TreatmentSheetLineDto> lines = new ArrayList<>();

    public void addLine(TreatmentSheetLineDto line){
        lines.add(line);
    }

    public void deleteLine(Long lineId){
        lines.removeIf(l -> l.getId().equals(lineId));
    }
}

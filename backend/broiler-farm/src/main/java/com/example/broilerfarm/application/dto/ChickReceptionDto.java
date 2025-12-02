package com.example.broilerfarm.application.dto;

import com.example.broilerfarm.domain.enums.ReceptionStatus;
import lombok.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ChickReceptionDto {
    @EqualsAndHashCode.Include
    private Long id;
    private LocalDateTime receptionDate;
    private Long farmId;
    private Long employeeid;
    private String transportConditions;
    private String truckInfo;
    private String referenceDocument;
    private ReceptionStatus receptionStatus;
    private Integer totalQuantityReceived;
    private Integer totalChicksAlive;
    private Integer totalChicksDoa;
    private Integer totalChicksWeak;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<ChicksReceptionLineDto> lines = new ArrayList<>();

    public void addLines(ChicksReceptionLineDto line){
        this.lines.add(line);
    }

    public void deleteLine(Long lineId){
            lines.removeIf(l -> l.getId().equals(lineId));
    }
}

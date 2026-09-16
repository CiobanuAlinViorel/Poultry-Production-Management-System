package com.example.broilerfarm.application.dto;
import com.example.broilerfarm.domain.enums.ApprovalStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ConsumableReceptionDto {
    @EqualsAndHashCode.Include
    private Long id;
    private String purchaseOrderRef;
    private String supplier;
    private Long receivingWarehouseId;
    private LocalDate receptionDate;
    private BigDecimal totalAmount;
    private Long receivingEmployeeId;
    private ApprovalStatus approvalStatus;
    private String notes;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<ConsReceptionLineDto>  consReceptionLines = new ArrayList<>();

    public void addLine(ConsReceptionLineDto consReceptionLineDto){
        consReceptionLines.add(consReceptionLineDto);
    }

    public void deleteLine(Long lineId){
        consReceptionLines.removeIf(l -> l.getId().equals(lineId));
    }
}

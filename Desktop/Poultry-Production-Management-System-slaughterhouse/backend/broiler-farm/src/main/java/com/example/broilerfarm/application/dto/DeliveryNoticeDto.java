package com.example.broilerfarm.application.dto;

import com.example.broilerfarm.domain.entities.FarmEmployee;
import com.example.broilerfarm.domain.enums.ApprovalStatus;
import com.example.broilerfarm.domain.enums.DataSource;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DeliveryNoticeDto {
    @EqualsAndHashCode.Include
    private Long id;
    private LocalDateTime scheduledDate;
    private Long farm;
    private String destination;
    private FarmEmployee transportManager;
    private String vehicleInfo;
    private LocalDateTime loadingTime;
    private String handlingRequirements;
    private ApprovalStatus approvalStatus;
    private Long approvedBy;
    private DataSource dataSource;
    private String slaughterhouseDock;
    private LocalDateTime transmissionTimestamp;
    private String specialInstructions;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<DeliveryNoticeLineDto> deliveryNoticeLines = new ArrayList<>();

    public void addLine(DeliveryNoticeLineDto deliveryNoticeLineDto){
        deliveryNoticeLines.add(deliveryNoticeLineDto);
    }

    public void deleteLine(Long lineId){
        deliveryNoticeLines.removeIf(l -> l.getId().equals(lineId));
    }
}

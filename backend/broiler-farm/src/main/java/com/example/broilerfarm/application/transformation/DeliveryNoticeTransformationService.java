package com.example.broilerfarm.application.transformation;

import com.example.broilerfarm.application.dto.DeliveryNoticeDto;
import com.example.broilerfarm.application.dto.DeliveryNoticeLineDto;
import com.example.broilerfarm.domain.entities.DeliveryNotice;
import com.example.broilerfarm.domain.entities.DeliveryNoticeLine;
import com.example.broilerfarm.infrastructure.persistence.repositories.BroilerFarmRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.ChicksLotRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.FarmEmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeliveryNoticeTransformationService {

    @Autowired
    private BroilerFarmRepository broilerFarmRepository;

    @Autowired
    private FarmEmployeeRepository farmEmployeeRepository;

    @Autowired
    private ChicksLotRepository chicksLotRepository;

    public DeliveryNoticeDto toDto(DeliveryNotice entity) {
        if (entity == null) {
            return null;
        }

        DeliveryNoticeDto dto = new DeliveryNoticeDto();
        dto.setId(entity.getId());
        dto.setScheduledDate(entity.getScheduledDate());
        dto.setFarm(entity.getFarm() != null ? entity.getFarm().getId() : null);
        dto.setDestination(entity.getDestination());
        dto.setTransportManager(entity.getTransportManager());
        dto.setVehicleInfo(entity.getVehicleInfo());
        dto.setLoadingTime(entity.getLoadingTime());
        dto.setHandlingRequirements(entity.getHandlingRequirements());
        dto.setApprovalStatus(entity.getApprovalStatus());
        dto.setApprovedBy(entity.getApprovedBy() != null ? entity.getApprovedBy().getId() : null);
        dto.setDataSource(entity.getDataSource());
        dto.setSlaughterhouseDock(entity.getSlaughterhouseDock());
        dto.setTransmissionTimestamp(entity.getTransmissionTimestamp());
        dto.setSpecialInstructions(entity.getSpecialInstructions());

        if (entity.getDeliveryLines() != null) {
            entity.getDeliveryLines().forEach(line -> dto.addLine(toLineDto(line)));
        }

        return dto;
    }

    public DeliveryNotice toEntity(DeliveryNoticeDto dto) {
        if (dto == null) {
            return null;
        }

        DeliveryNotice entity = new DeliveryNotice();
        entity.setId(dto.getId());
        entity.setScheduledDate(dto.getScheduledDate());

        if (dto.getFarm() != null) {
            entity.setFarm(broilerFarmRepository.findById(dto.getFarm()).orElse(null));
        }

        entity.setDestination(dto.getDestination());
        entity.setTransportManager(dto.getTransportManager());
        entity.setVehicleInfo(dto.getVehicleInfo());
        entity.setLoadingTime(dto.getLoadingTime());
        entity.setHandlingRequirements(dto.getHandlingRequirements());
        entity.setApprovalStatus(dto.getApprovalStatus());

        if (dto.getApprovedBy() != null) {
            entity.setApprovedBy(farmEmployeeRepository.findById(dto.getApprovedBy()).orElse(null));
        }

        entity.setDataSource(dto.getDataSource());
        entity.setSlaughterhouseDock(dto.getSlaughterhouseDock());
        entity.setTransmissionTimestamp(dto.getTransmissionTimestamp());
        entity.setSpecialInstructions(dto.getSpecialInstructions());

        return entity;
    }

    public DeliveryNoticeLineDto toLineDto(DeliveryNoticeLine entity) {
        if (entity == null) {
            return null;
        }

        DeliveryNoticeLineDto dto = new DeliveryNoticeLineDto();
        dto.setId(entity.getId());
        dto.setDeliveryNoticeId(entity.getDeliveryNotice() != null ? entity.getDeliveryNotice().getId() : null);
        dto.setLotNumber(entity.getLot() != null ? entity.getLot().getLotNumber() : null);
        dto.setEstimatedQuantity(entity.getEstimatedQuantity());
        dto.setAverageWeight(entity.getAverageWeight());
        dto.setQualityGrade(entity.getQualityGrade());
        dto.setSpecialInstructions(entity.getSpecialInstructions());
        dto.setLoadingBay(entity.getLoadingBay());
        dto.setActualQuantityDelivered(entity.getActualQuantityDelivered());
        dto.setActualAverageWeight(entity.getActualAverageWeight());

        return dto;
    }

    public DeliveryNoticeLine toLineEntity(DeliveryNoticeLineDto dto) {
        if (dto == null) {
            return null;
        }

        DeliveryNoticeLine entity = DeliveryNoticeLine.builder()
                .estimatedQuantity(dto.getEstimatedQuantity())
                .averageWeight(dto.getAverageWeight())
                .qualityGrade(dto.getQualityGrade())
                .specialInstructions(dto.getSpecialInstructions())
                .loadingBay(dto.getLoadingBay())
                .actualQuantityDelivered(dto.getActualQuantityDelivered())
                .actualAverageWeight(dto.getActualAverageWeight())
                .build();

        entity.setId(dto.getId());

        if (dto.getLotNumber() != null) {
            entity.setLot(chicksLotRepository.findById(dto.getLotNumber()).orElse(null));
        }

        return entity;
    }
}
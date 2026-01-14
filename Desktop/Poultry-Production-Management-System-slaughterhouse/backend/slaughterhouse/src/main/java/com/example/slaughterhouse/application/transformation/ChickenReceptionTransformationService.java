package com.example.slaughterhouse.application.transformation;

import com.example.slaughterhouse.application.dto.ChickenReceptionDto;
import com.example.slaughterhouse.domain.entities.ChickenReception;
import com.example.slaughterhouse.domain.entities.DeliveryNotice;
import com.example.slaughterhouse.domain.entities.SlaughterLot;
import com.example.slaughterhouse.domain.entities.SlaughterhouseUser;
import com.example.slaughterhouse.infrastructure.persistance.repository.DeliveryNoticeRepository;
import com.example.slaughterhouse.infrastructure.persistance.repository.SlaughterLotRepository;
import com.example.slaughterhouse.infrastructure.persistance.repository.SlaughterhouseUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChickenReceptionTransformationService {
    
    @Autowired
    private SlaughterLotRepository slaughterLotRepository;
    
    @Autowired
    private DeliveryNoticeRepository deliveryNoticeRepository;
    
    @Autowired
    private SlaughterhouseUserRepository userRepository;
    
    public ChickenReceptionDto toDto(ChickenReception entity) {
        if (entity == null) {
            return null;
        }
        
        ChickenReceptionDto dto = new ChickenReceptionDto();
        dto.setId(entity.getId());
        dto.setSlaughterLotId(entity.getSlaughterLot() != null ? entity.getSlaughterLot().getId() : null);
        dto.setDeliveryNoticeId(entity.getDeliveryNotice() != null ? entity.getDeliveryNotice().getId() : null);
        dto.setReceptionDate(entity.getReceptionDate());
        dto.setReceptionTime(entity.getReceptionTime());
        dto.setReceivedById(entity.getReceivedBy() != null ? entity.getReceivedBy().getId() : null);
        dto.setQuantityReceived(entity.getQuantityReceived());
        dto.setChicksAlive(entity.getChicksAlive());
        dto.setChicksDOA(entity.getChicksDOA());
        dto.setTransportConditions(entity.getTransportConditions());
        dto.setAnimalWelfareCheck(entity.getAnimalWelfareCheck());
        dto.setAnimalWelfareNotes(entity.getAnimalWelfareNotes());
        dto.setNotes(entity.getNotes());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        
        return dto;
    }
    
    public ChickenReception toEntity(ChickenReceptionDto dto) {
        if (dto == null) {
            return null;
        }
        
        ChickenReception entity = new ChickenReception();
        entity.setId(dto.getId());
        
        // Set SlaughterLot
        if (dto.getSlaughterLotId() != null) {
            SlaughterLot lot = slaughterLotRepository.findById(dto.getSlaughterLotId()).orElse(null);
            entity.setSlaughterLot(lot);
        }
        
        // Set DeliveryNotice
        if (dto.getDeliveryNoticeId() != null) {
            DeliveryNotice notice = deliveryNoticeRepository.findById(dto.getDeliveryNoticeId()).orElse(null);
            entity.setDeliveryNotice(notice);
        }
        
        // Set ReceivedBy
        if (dto.getReceivedById() != null) {
            SlaughterhouseUser user = userRepository.findById(dto.getReceivedById()).orElse(null);
            entity.setReceivedBy(user);
        }
        
        entity.setReceptionDate(dto.getReceptionDate());
        entity.setReceptionTime(dto.getReceptionTime());
        entity.setQuantityReceived(dto.getQuantityReceived());
        entity.setChicksAlive(dto.getChicksAlive());
        entity.setChicksDOA(dto.getChicksDOA());
        entity.setTransportConditions(dto.getTransportConditions());
        entity.setAnimalWelfareCheck(dto.getAnimalWelfareCheck() != null ? dto.getAnimalWelfareCheck() : false);
        entity.setAnimalWelfareNotes(dto.getAnimalWelfareNotes());
        entity.setNotes(dto.getNotes());
        entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        
        return entity;
    }
}

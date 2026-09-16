package com.example.broilerfarm.application.transformation;

import com.example.broilerfarm.application.dto.PoultryHouseDto;
import com.example.broilerfarm.domain.entities.ChicksLot;
import com.example.broilerfarm.domain.entities.PoultryHouse;
import com.example.broilerfarm.infrastructure.persistence.repositories.BroilerFarmRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.ChicksLotRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.PoultryHouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PoultryHouseTransformationService {

    @Autowired
    private BroilerFarmRepository broilerFarmRepository;

    @Autowired
    private ChicksLotRepository chicksLotRepository;

    public PoultryHouseDto toDto(PoultryHouse entity) {
        if (entity == null) {
            return null;
        }

        return new PoultryHouseDto(
                entity.getId(),
                entity.getFarm() != null ? entity.getFarm().getId() : null,
                entity.getCapacity(),
                entity.getCurrentLot().getLotNumber(),
                entity.getArea(),
                entity.getType(),
                entity.getEquipmentType(),
                entity.getStatus(),
                entity.getCurrentOccupancy()
        );
    }

    public PoultryHouse toEntity(PoultryHouseDto dto) {
        if (dto == null) {
            return null;
        }

        ChicksLot chicksLot = chicksLotRepository.findByLotNumber(dto.getCurrentLot());

        if(chicksLot == null) {
            throw new RuntimeException("chicksLot not found");
        }

        PoultryHouse entity = new PoultryHouse();
        entity.setId(dto.getId());
        entity.setCapacity(dto.getCapacity());
        entity.setCurrentLot(chicksLot);
        entity.setArea(dto.getArea());
        entity.setType(dto.getType());
        entity.setEquipmentType(dto.getEquipmentType());
        entity.setStatus(dto.getStatus());
        entity.setCurrentOccupancy(dto.getCurrentOccupancy());

        if (dto.getFarmId() != null) {
            entity.setFarm(broilerFarmRepository.findById(dto.getFarmId()).orElse(null));
        }

        return entity;
    }
}
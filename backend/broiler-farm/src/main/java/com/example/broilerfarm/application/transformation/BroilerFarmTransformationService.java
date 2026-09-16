package com.example.broilerfarm.application.transformation;

import com.example.broilerfarm.application.dto.BroilerFarmDto;
import com.example.broilerfarm.domain.entities.BroilerFarm;
import org.springframework.stereotype.Service;

@Service
public class BroilerFarmTransformationService {

    public BroilerFarmDto toDto(BroilerFarm entity) {
        if (entity == null) {
            return null;
        }

        return new BroilerFarmDto(
                entity.getId(),
                entity.getFarmName(),
                entity.getLocation(),
                entity.getAddress(),
                entity.getCapacity(),
                entity.getLicenseNumber()
        );
    }

    public BroilerFarm toEntity(BroilerFarmDto dto) {
        if (dto == null) {
            return null;
        }

        BroilerFarm entity = new BroilerFarm(
                dto.getFarmName(),
                dto.getLocation(),
                dto.getAddress(),
                dto.getCapacity(),
                dto.getLicenseNumber()
        );

        entity.setId(dto.getId());

        return entity;
    }
}
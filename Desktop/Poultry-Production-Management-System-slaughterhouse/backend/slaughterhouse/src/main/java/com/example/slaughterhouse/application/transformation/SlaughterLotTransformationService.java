package com.example.slaughterhouse.application.transformation;

import com.example.slaughterhouse.application.dto.SlaughterLotDto;
import com.example.slaughterhouse.domain.entities.SlaughterLot;
import com.example.slaughterhouse.domain.entities.SlaughterhouseEmployee;
import com.example.slaughterhouse.domain.valueobjects.HealthStatus;
import com.example.slaughterhouse.domain.valueobjects.QualityGrade;
import com.example.slaughterhouse.domain.valueobjects.Weight;
import com.example.slaughterhouse.infrastructure.persistance.repository.SlaughterhouseEmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class SlaughterLotTransformationService {
    
    @Autowired
    private SlaughterhouseEmployeeRepository employeeRepository;
    
    @Autowired
    private ProductTransformationService productTransformationService;
    
    public SlaughterLotDto toDto(SlaughterLot entity) {
        if (entity == null) {
            return null;
        }
        
        SlaughterLotDto dto = new SlaughterLotDto();
        dto.setId(entity.getId());
        dto.setLotNumber(entity.getLotNumber());
        dto.setBreed(entity.getBreed());
        dto.setSlaughterDate(entity.getSlaughterDate());
        dto.setTotalChickens(entity.getTotalChickens());
        dto.setCurrentQuantity(entity.getCurrentQuantity());
        dto.setAverageAgeInDays(entity.getAverageAgeInDays());
        dto.setStatus(entity.getStatus());
        
        // Average Weight mapping
        if (entity.getAverageWeight() != null) {
            dto.setAverageWeightValue(entity.getAverageWeight().getValue());
            dto.setAverageWeightUnit(entity.getAverageWeight().getUnit());
        }
        
        // Total Weight mapping
        if (entity.getTotalWeight() != null) {
            dto.setTotalWeightValue(entity.getTotalWeight().getValue());
            dto.setTotalWeightUnit(entity.getTotalWeight().getUnit());
        }
        
        // Health Status mapping
        if (entity.getHealthStatus() != null) {
            dto.setHealthStatus(entity.getHealthStatus().getStatus());
            dto.setHealthDescription(entity.getHealthStatus().getDescription());
            dto.setHealthSeverity(entity.getHealthStatus().getSeverity());
        }
        
        // Quality Grade mapping
        if (entity.getQualityGrade() != null) {
            dto.setQualityGrade(entity.getQualityGrade().getGrade());
            dto.setQualityScore(entity.getQualityGrade().getScore() != null ?
                    entity.getQualityGrade().getScore().doubleValue() : null);
            dto.setQualityDescription(entity.getQualityGrade().getDescription());
        }
        
        dto.setManagerId(entity.getManager() != null ? entity.getManager().getId() : null);
        dto.setIsActive(entity.getIsActive());
        
        // Products - optional, only if you want full representation
        if (entity.getProducts() != null && !entity.getProducts().isEmpty()) {
            dto.setProducts(entity.getProducts().stream()
                    .map(productTransformationService::toDto)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }

    public SlaughterLot toEntity(SlaughterLotDto dto) {
        if (dto == null) {
            return null;
        }

        SlaughterLot entity = new SlaughterLot();
        entity.setId(dto.getId());
        entity.setLotNumber(dto.getLotNumber());
        entity.setBreed(dto.getBreed());
        entity.setSlaughterDate(dto.getSlaughterDate());

        // ⭐ Set total_chickens with default if null
        entity.setTotalChickens(dto.getTotalChickens() != null ? dto.getTotalChickens() : 0);

        // ⭐ Set current_quantity with default (same as total_chickens or 0)
        entity.setCurrentQuantity(dto.getCurrentQuantity() != null ? dto.getCurrentQuantity() :
                (dto.getTotalChickens() != null ? dto.getTotalChickens() : 0));

        entity.setStatus(dto.getStatus());

        // Average Weight mapping
        if (dto.getAverageWeightValue() != null) {
            String unit = dto.getAverageWeightUnit() != null ? dto.getAverageWeightUnit() : "kg";
            entity.setAverageWeight(Weight.of(dto.getAverageWeightValue().floatValue(), unit));
        }

        // Total Weight mapping
        if (dto.getTotalWeightValue() != null) {
            String unit = dto.getTotalWeightUnit() != null ? dto.getTotalWeightUnit() : "kg";
            entity.setTotalWeight(Weight.of(dto.getTotalWeightValue().floatValue(), unit));
        }

        // Quality Grade mapping
        if (dto.getQualityGrade() != null) {
            entity.setQualityGrade(new QualityGrade(
                    dto.getQualityGrade(),
                    dto.getQualityScore() != null ? dto.getQualityScore().intValue() : null,
                    dto.getQualityDescription()
            ));
        }

        // Health Status mapping
        if (dto.getHealthStatus() != null) {
            entity.setHealthStatus(new HealthStatus(
                    dto.getHealthStatus(),
                    dto.getHealthSeverity(),
                    dto.getHealthDescription()
            ));
        }

        entity.setAverageAgeInDays(dto.getAverageAgeInDays());
        entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        return entity;
    }
}

package com.example.slaughterhouse.application.transformation;

import com.example.slaughterhouse.application.dto.ProductDto;
import com.example.slaughterhouse.domain.entities.Product;
import com.example.slaughterhouse.domain.entities.SlaughterLot;
import com.example.slaughterhouse.domain.valueobjects.QualityGrade;
import com.example.slaughterhouse.domain.valueobjects.Weight;
import com.example.slaughterhouse.infrastructure.persistance.repository.SlaughterLotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductTransformationService {

    @Autowired
    private SlaughterLotRepository slaughterLotRepository;

    public ProductDto toDto(Product entity) {
        if (entity == null) {
            return null;
        }

        ProductDto dto = new ProductDto();
        dto.setId(entity.getId());
        dto.setSlaughterLotId(entity.getSlaughterLot() != null ? entity.getSlaughterLot().getId() : null);
        dto.setProductType(entity.getProductType());
        dto.setCut(entity.getCut());

        // Weight mapping
        if (entity.getWeight() != null) {
            dto.setWeightValue(entity.getWeight().getValue() != null ? entity.getWeight().getValue().doubleValue() : null);
            dto.setWeightUnit(entity.getWeight().getUnit());
        }

        // Quality Grade mapping
        if (entity.getQualityGrade() != null) {
            dto.setQualityGrade(entity.getQualityGrade().getGrade());
            dto.setQualityScore(entity.getQualityGrade().getScore() != null ?
                    entity.getQualityGrade().getScore().doubleValue() : null);
            dto.setQualityDescription(entity.getQualityGrade().getDescription());
        }

        dto.setProductionDate(entity.getProductionDate());
        dto.setStatus(entity.getStatus());
        dto.setBatchNumber(entity.getBatchNumber());
        dto.setInspectionPassed(entity.getInspectionPassed());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setIsActive(entity.getIsActive());

        return dto;
    }

    public Product toEntity(ProductDto dto) {
        if (dto == null) {
            return null;
        }

        Product entity = new Product();
        entity.setId(dto.getId());

        // Set SlaughterLot
        if (dto.getSlaughterLotId() != null) {
            SlaughterLot slaughterLot = slaughterLotRepository.findById(dto.getSlaughterLotId()).orElse(null);
            entity.setSlaughterLot(slaughterLot);
        }

        entity.setProductType(dto.getProductType());
        entity.setCut(dto.getCut());

        // Weight mapping - use default unit if not provided
        if (dto.getWeightValue() != null) {
            String unit = dto.getWeightUnit() != null ? dto.getWeightUnit() : "kg";
            entity.setWeight(Weight.of(dto.getWeightValue().floatValue(), unit));
        }

        // Quality Grade mapping
        if (dto.getQualityGrade() != null) {
            entity.setQualityGrade(new QualityGrade(
                    dto.getQualityGrade(),
                    dto.getQualityScore() != null ? dto.getQualityScore().intValue() : null,
                    dto.getQualityDescription()
            ));
        }

        // ⭐ Set production date - use current date if not provided
        entity.setProductionDate(dto.getProductionDate() != null ? dto.getProductionDate() : java.time.LocalDate.now());

        entity.setStatus(dto.getStatus());
        entity.setBatchNumber(dto.getBatchNumber());

        // ⭐ Set default value for inspection_passed if null
        entity.setInspectionPassed(dto.getInspectionPassed() != null ? dto.getInspectionPassed() : false);

        entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        return entity;
    }
    }

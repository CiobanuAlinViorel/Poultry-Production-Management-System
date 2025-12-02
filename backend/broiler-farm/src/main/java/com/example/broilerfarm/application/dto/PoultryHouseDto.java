package com.example.broilerfarm.application.dto;
import com.example.broilerfarm.domain.enums.PoultryHouseStatus;
import com.example.broilerfarm.domain.enums.PoultryHouseType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PoultryHouseDto {
    private Long id;
    private Long farmId;
    private Integer capacity;
    private Integer currentLot;
    private Double area;
    private PoultryHouseType type;
    private String equipmentType;
    private PoultryHouseStatus status;
    private Integer currentOccupancy;
}

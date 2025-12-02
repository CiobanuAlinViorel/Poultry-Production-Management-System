package com.example.broilerfarm.application.dto;

import com.example.broilerfarm.domain.enums.ChicksLotStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class ChicksLotDto {
    private String lotNumber;
    private Long houseId;
    private String hatcherySource;
    private String breed;
    private LocalDate receptionDate;
    private Integer initialQuantity;
    private Integer currentQuantity;
    private LocalDate expectedSlaughterDate;
    private LocalDate actualSlaughterDate;
    private ChicksLotStatus status;
    private Double expectedMortalityRate;
}

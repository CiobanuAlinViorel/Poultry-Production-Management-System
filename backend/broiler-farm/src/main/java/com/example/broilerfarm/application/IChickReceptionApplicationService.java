package com.example.broilerfarm.application;

import com.example.broilerfarm.application.dto.ChickReceptionDto;
import com.example.broilerfarm.domain.enums.QualityGrade;

import java.time.LocalDateTime;
import java.util.List;

public interface IChickReceptionApplicationService {

    // (1) Create Reception
    ChickReceptionDto createReception(
            LocalDateTime receptionDate,
            Long farmId,
            Long employeeId,
            String transportConditions,
            String truckInfo,
            String referenceDocument
    );

    // (2) Add Reception Line
    ChickReceptionDto addReceptionLine(
            Long receptionId,
            Long poultryHouseId,
            Integer chicksAlive,
            Integer chicksDOA,
            Integer chicksWeak,
            QualityGrade qualityGrade,
            String notes
    );

    // (3) Update Reception Line
    ChickReceptionDto updateReceptionLine(
            Long lineId,
            Integer chicksAlive,
            Integer chicksDOA,
            Integer chicksWeak
    );

    // (4) Delete Reception Line
    ChickReceptionDto deleteReceptionLine(Long lineId);

    // (5) Update Reception
    ChickReceptionDto updateReception(
            Long receptionId,
            LocalDateTime receptionDate,
            Long employeeId,
            String transportConditions,
            String truckInfo,
            String referenceDocument
    );

    // (6) Create ChicksLots from all reception lines
    ChickReceptionDto createChicksLots(
            Long receptionId,
            String breed,
            String hatcherySource
    );

    // (7) Finalize Reception
    ChickReceptionDto finalizeReception(Long receptionId);

    // (8) Get Reception by ID
    ChickReceptionDto getReceptionById(Long receptionId);

    // (9) Get all Receptions for a Farm
    List<ChickReceptionDto> getReceptionsByFarm(Long farmId);
}
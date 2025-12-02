package com.example.broilerfarm.application;

import com.example.broilerfarm.application.IChickReceptionApplicationService;
import com.example.broilerfarm.application.dto.ChickReceptionDto;
import com.example.broilerfarm.domain.entities.*;
import com.example.broilerfarm.domain.enums.QualityGrade;
import com.example.broilerfarm.domain.enums.ReceptionStatus;
import com.example.broilerfarm.infrastructure.persistence.repositories.*;
import com.example.broilerfarm.services.ChicksLotFactory;
import com.example.broilerfarm.services.ChicksReceptionServiceImplementation;
import com.example.broilerfarm.services.ChicksReceptionTransformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ChicksReceptionApplicationService implements IChickReceptionApplicationService {

    @Autowired
    private ChicksReceptionRepository chicksReceptionRepository;

    @Autowired
    private BroilerFarmRepository broilerFarmRepository;

    @Autowired
    private FarmEmployeeRepository farmEmployeeRepository;

    @Autowired
    private ChicksReceptionServiceImplementation chicksReceptionService;

    @Autowired
    private ChicksLotFactory chicksLotFactory;

    @Autowired
    private ChicksReceptionTransformationService transformationService;

    @Autowired
    private ChicksReceptionLineRepository chicksReceptionLineRepository;

    @Override
    public ChickReceptionDto createReception(
            LocalDateTime receptionDate,
            Long farmId,
            Long employeeId,
            String transportConditions,
            String truckInfo,
            String referenceDocument
    ) {
        if (farmId == null) {
            throw new IllegalArgumentException("Farm ID cannot be null");
        }

        if (employeeId == null) {
            throw new IllegalArgumentException("Employee ID cannot be null");
        }

        if (receptionDate == null) {
            throw new IllegalArgumentException("Reception date cannot be null");
        }

        BroilerFarm farm = broilerFarmRepository.findById(farmId).orElse(null);
        if (farm == null) {
            throw new IllegalArgumentException("Farm not found");
        }

        FarmEmployee employee = farmEmployeeRepository.findById(employeeId).orElse(null);
        if (employee == null) {
            throw new IllegalArgumentException("Employee not found");
        }

        ChicksReception reception = ChicksReception.builder()
                .receptionDate(receptionDate)
                .farm(farm)
                .receivingEmployee(employee)
                .transportConditions(transportConditions)
                .truckInfo(truckInfo)
                .documentReference(referenceDocument)
                .status(ReceptionStatus.DRAFT)
                .totalQuantityReceived(0)
                .totalChicksAlive(0)
                .totalChicksDOA(0)
                .totalChicksWeak(0)
                .build();

        reception = chicksReceptionRepository.save(reception);
        return transformationService.toDto(reception);
    }

    @Override
    public ChickReceptionDto addReceptionLine(
            Long receptionId,
            Long poultryHouseId,
            Integer chicksAlive,
            Integer chicksDOA,
            Integer chicksWeak,
            QualityGrade qualityGrade,
            String notes
    ) {
        chicksReceptionService.addLine(
                receptionId,
                poultryHouseId,
                chicksAlive,
                chicksDOA,
                chicksWeak,
                qualityGrade,
                notes
        );

        ChicksReception reception = chicksReceptionRepository.findById(receptionId).orElse(null);
        if (reception == null) {
            throw new IllegalArgumentException("Reception not found");
        }

        return transformationService.toDto(reception);
    }

    @Override
    public ChickReceptionDto updateReceptionLine(
            Long lineId,
            Integer chicksAlive,
            Integer chicksDOA,
            Integer chicksWeak
    ) {
        chicksReceptionService.updateLine(lineId, chicksAlive, chicksDOA, chicksWeak);

        ChicksReceptionLine line = chicksReceptionLineRepository.findById(lineId).orElse(null);
        if (line == null) {
            throw new IllegalArgumentException("Reception line not found");
        }

        return transformationService.toDto(line.getReception());
    }

    @Override
    public ChickReceptionDto deleteReceptionLine(Long lineId) {
        ChicksReceptionLine line = chicksReceptionLineRepository.findById(lineId).orElse(null);
        if (line == null) {
            throw new IllegalArgumentException("Reception line not found");
        }

        ChicksReception reception = line.getReception();
        Long receptionId = reception.getId();

        chicksReceptionService.deleteLine(lineId);

        reception = chicksReceptionRepository.findById(receptionId).orElse(null);
        return transformationService.toDto(reception);
    }

    @Override
    public ChickReceptionDto updateReception(
            Long receptionId,
            LocalDateTime receptionDate,
            Long employeeId,
            String transportConditions,
            String truckInfo,
            String referenceDocument
    ) {
        if (receptionId == null) {
            throw new IllegalArgumentException("Reception ID cannot be null");
        }

        ChicksReception reception = chicksReceptionRepository.findById(receptionId).orElse(null);
        if (reception == null) {
            throw new IllegalArgumentException("Reception not found");
        }

        if (reception.getStatus() != ReceptionStatus.DRAFT) {
            throw new IllegalStateException("Cannot update finalized reception");
        }

        if (receptionDate != null) {
            reception.setReceptionDate(receptionDate);
        }

        if (employeeId != null) {
            FarmEmployee employee = farmEmployeeRepository.findById(employeeId).orElse(null);
            if (employee == null) {
                throw new IllegalArgumentException("Employee not found");
            }
            reception.setReceivingEmployee(employee);
        }

        if (transportConditions != null) {
            reception.setTransportConditions(transportConditions);
        }

        if (truckInfo != null) {
            reception.setTruckInfo(truckInfo);
        }

        if (referenceDocument != null) {
            reception.setDocumentReference(referenceDocument);
        }

        reception = chicksReceptionRepository.save(reception);
        return transformationService.toDto(reception);
    }

    @Override
    public ChickReceptionDto createChicksLots(Long receptionId, String breed, String hatcherySource) {
        if (receptionId == null) {
            throw new IllegalArgumentException("Reception ID cannot be null");
        }

        if (breed == null || breed.isEmpty()) {
            throw new IllegalArgumentException("Breed cannot be null or empty");
        }

        if (hatcherySource == null || hatcherySource.isEmpty()) {
            throw new IllegalArgumentException("Hatchery source cannot be null or empty");
        }

        ChicksReception reception = chicksReceptionRepository.findById(receptionId).orElse(null);
        if (reception == null) {
            throw new IllegalArgumentException("Reception not found");
        }

        if (reception.getStatus() != ReceptionStatus.DRAFT) {
            throw new IllegalStateException("Can only create lots from DRAFT reception");
        }

        if (reception.getReceptionLines().isEmpty()) {
            throw new IllegalStateException("Cannot create lots without reception lines");
        }

        for (ChicksReceptionLine line : reception.getReceptionLines()) {
            if (line.getPoultryHouse() == null) {
                throw new IllegalStateException("All lines must have poultry house assigned");
            }

            if (!line.isLotCreated()) {
                ChicksLot lot = chicksLotFactory.createLot(line, breed, hatcherySource);
                line.setCreatedLot(lot);
            }
        }

        reception.finalizeReception();
        reception = chicksReceptionRepository.save(reception);

        return transformationService.toDto(reception);
    }

    @Override
    public ChickReceptionDto finalizeReception(Long receptionId) {
        if (receptionId == null) {
            throw new IllegalArgumentException("Reception ID cannot be null");
        }

        ChicksReception reception = chicksReceptionRepository.findById(receptionId).orElse(null);
        if (reception == null) {
            throw new IllegalArgumentException("Reception not found");
        }

        reception.finalizeReception();
        reception = chicksReceptionRepository.save(reception);

        return transformationService.toDto(reception);
    }

    @Override
    public ChickReceptionDto getReceptionById(Long receptionId) {
        if (receptionId == null) {
            throw new IllegalArgumentException("Reception ID cannot be null");
        }

        ChicksReception reception = chicksReceptionRepository.findById(receptionId).orElse(null);
        if (reception == null) {
            throw new IllegalArgumentException("Reception not found");
        }

        return transformationService.toDto(reception);
    }

    @Override
    public List<ChickReceptionDto> getReceptionsByFarm(Long farmId) {
        if (farmId == null) {
            throw new IllegalArgumentException("Farm ID cannot be null");
        }

        BroilerFarm farm = broilerFarmRepository.findById(farmId).orElse(null);
        if (farm == null) {
            throw new IllegalArgumentException("Farm not found");
        }

        List<ChicksReception> receptions = chicksReceptionRepository.findByFarmId(farm.getId());
        return receptions.stream()
                .map(transformationService::toDto)
                .toList();
    }
}
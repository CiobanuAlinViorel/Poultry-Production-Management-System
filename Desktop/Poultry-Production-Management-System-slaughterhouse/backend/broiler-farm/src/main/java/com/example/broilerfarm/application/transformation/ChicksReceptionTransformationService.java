package com.example.broilerfarm.application.transformation;

import com.example.broilerfarm.application.dto.ChickReceptionDto;
import com.example.broilerfarm.application.dto.ChicksReceptionLineDto;
import com.example.broilerfarm.domain.entities.ChicksReception;
import com.example.broilerfarm.domain.entities.ChicksReceptionLine;
import com.example.broilerfarm.infrastructure.persistence.repositories.BroilerFarmRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.ChicksLotRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.FarmEmployeeRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.PoultryHouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class ChicksReceptionTransformationService {

    @Autowired
    private BroilerFarmRepository broilerFarmRepository;

    @Autowired
    private FarmEmployeeRepository farmEmployeeRepository;

    @Autowired
    private PoultryHouseRepository poultryHouseRepository;

    @Autowired
    private ChicksLotRepository chicksLotRepository;

    public ChickReceptionDto toDto(ChicksReception entity) {
        if (entity == null) {
            return null;
        }

        ChickReceptionDto dto = new ChickReceptionDto();
        dto.setId(entity.getId());
        dto.setReceptionDate(entity.getReceptionDate());
        dto.setFarmId(entity.getFarm() != null ? entity.getFarm().getId() : null);
        dto.setEmployeeid(entity.getReceivingEmployee() != null ? entity.getReceivingEmployee().getId() : null);
        dto.setTransportConditions(entity.getTransportConditions());
        dto.setTruckInfo(entity.getTruckInfo());
        dto.setReferenceDocument(entity.getDocumentReference());
        dto.setReceptionStatus(entity.getStatus());
        dto.setTotalQuantityReceived(entity.getTotalQuantityReceived());
        dto.setTotalChicksAlive(entity.getTotalChicksAlive());
        dto.setTotalChicksDoa(entity.getTotalChicksDOA());
        dto.setTotalChicksWeak(entity.getTotalChicksWeak());

        if (entity.getReceptionLines() != null) {
            entity.getReceptionLines().forEach(line -> dto.addLines(toLineDto(line)));
        }

        return dto;
    }

    public ChicksReception toEntity(ChickReceptionDto dto) {
        ChicksReception entity = ChicksReception.builder()
                .receptionDate(dto.getReceptionDate())
                .transportConditions(dto.getTransportConditions())
                .truckInfo(dto.getTruckInfo())
                .documentReference(dto.getReferenceDocument())
                .status(dto.getReceptionStatus())
                .totalQuantityReceived(dto.getTotalQuantityReceived())
                .totalChicksAlive(dto.getTotalChicksAlive())
                .totalChicksDOA(dto.getTotalChicksDoa())
                .totalChicksWeak(dto.getTotalChicksWeak())
                .build();

        entity.setId(dto.getId());

        // ASIGURĂ-TE că lista este inițializată
        if (entity.getReceptionLines() == null) {
            entity.setReceptionLines(new ArrayList<>());
        }
        // Setează farm
        if (dto.getFarmId() != null) {
            entity.setFarm(broilerFarmRepository.findById(dto.getFarmId()).orElse(null));
        }

        // Setează employee
        if (dto.getEmployeeid() != null) {
            entity.setReceivingEmployee(farmEmployeeRepository.findById(dto.getEmployeeid()).orElse(null));
        }



        return entity;
    }

    public ChicksReceptionLineDto toLineDto(ChicksReceptionLine entity) {
        if (entity == null) {
            return null;
        }

        ChicksReceptionLineDto dto = new ChicksReceptionLineDto();
        dto.setId(entity.getId());
        dto.setReceptionId(entity.getReception() != null ? entity.getReception().getId() : null);
        dto.setPoultryHouseId(entity.getPoultryHouse() != null ? entity.getPoultryHouse().getId() : null);
        dto.setLotNumber(entity.getCreatedLot() != null ? entity.getCreatedLot().getLotNumber() : null);
        dto.setQuantity(entity.getQuantity());
        dto.setChicksAlive(entity.getChicksAlive());
        dto.setChicksDOA(entity.getChicksDOA());
        dto.setChicksWeak(entity.getChicksWeak());
        dto.setQualityGrade(entity.getQualityGrade());
        dto.setNotes(entity.getNotes());

        return dto;
    }

    public ChicksReceptionLine toLineEntity(ChicksReceptionLineDto dto) {
        if (dto == null) {
            return null;
        }

        ChicksReceptionLine entity = ChicksReceptionLine.builder()
                .quantity(dto.getQuantity())
                .chicksAlive(dto.getChicksAlive())
                .chicksDOA(dto.getChicksDOA())
                .chicksWeak(dto.getChicksWeak())
                .qualityGrade(dto.getQualityGrade())
                .notes(dto.getNotes())
                .breed(dto.getBreed())  // ← ADAUGAT (dacă există în entitate)
                .hatcherySource(dto.getHatcherySource())  // ← ADAUGAT (dacă există)
                .build();

        entity.setId(dto.getId());

        if (dto.getPoultryHouseId() != null) {
            entity.setPoultryHouse(poultryHouseRepository.findById(dto.getPoultryHouseId()).orElse(null));
        }

        if (dto.getLotNumber() != null) {
            entity.setCreatedLot(chicksLotRepository.findByLotNumber(dto.getLotNumber()));
        }

        // ✅ IMPORTANT: Nu setezi reception aici, se va seta în toEntity()
        // entity.setReception(...);  // ← NU face asta aici!

        return entity;
    }
}
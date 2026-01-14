package com.example.broilerfarm.application;

import com.example.broilerfarm.application.dto.PoultryHouseDto;
import com.example.broilerfarm.domain.entities.BroilerFarm;
import com.example.broilerfarm.domain.entities.PoultryHouse;
import com.example.broilerfarm.domain.enums.PoultryHouseStatus;

import com.example.broilerfarm.infrastructure.persistence.repositories.BroilerFarmRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.PoultryHouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Application Service for Poultry House CRUD operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PoultryHouseApplicationService {

    private final PoultryHouseRepository poultryHouseRepository;
    private final BroilerFarmRepository broilerFarmRepository;

    /**
     * Create new poultry house
     */
    public PoultryHouseDto createHouse(PoultryHouseDto dto) {
        log.info("Creating poultry house with capacity: {}", dto.getCapacity());

        // Validation
        validateHouseDto(dto);

        // Find farm
        BroilerFarm farm = broilerFarmRepository.findById(dto.getFarmId())
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id: " + dto.getFarmId()));

        // Create entity
        PoultryHouse house = new PoultryHouse();
        house.setFarm(farm);
        house.setCapacity(dto.getCapacity());
        house.setArea(dto.getArea());
        house.setType(dto.getType());
        house.setEquipmentType(dto.getEquipmentType());
        house.setStatus(dto.getStatus() != null ? dto.getStatus() : PoultryHouseStatus.EMPTY);
        house.setCurrentOccupancy(0); // Start with 0

        // Save
        house = poultryHouseRepository.save(house);
        log.info("Poultry house created with id: {}", house.getId());

        return toDto(house);
    }

    /**
     * Get all poultry houses
     */
    @Transactional(readOnly = true)
    public List<PoultryHouseDto> getAllHouses() {
        return poultryHouseRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get house by ID
     */
    @Transactional(readOnly = true)
    public PoultryHouseDto getHouseById(Long id) {
        PoultryHouse house = poultryHouseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("House not found with id: " + id));
        return toDto(house);
    }

    /**
     * Get houses by farm
     */
    @Transactional(readOnly = true)
    public List<PoultryHouseDto> getHousesByFarm(Long farmId) {
        BroilerFarm farm = broilerFarmRepository.findById(farmId)
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id: " + farmId));

        return poultryHouseRepository.findByFarmId(farm.getId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Update poultry house
     */
    public PoultryHouseDto updateHouse(Long id, PoultryHouseDto dto) {
        log.info("Updating poultry house {}", id);

        PoultryHouse house = poultryHouseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("House not found with id: " + id));

        // Update fields
        if (dto.getCapacity() != null && dto.getCapacity() > 0) {
            house.setCapacity(dto.getCapacity());
        }
        if (dto.getArea() != null && dto.getArea() > 0) {
            house.setArea(dto.getArea());
        }
        if (dto.getType() != null) {
            house.setType(dto.getType());
        }
        if (dto.getEquipmentType() != null) {
            house.setEquipmentType(dto.getEquipmentType());
        }
        if (dto.getStatus() != null) {
            house.setStatus(dto.getStatus());
        }
        if (dto.getCurrentOccupancy() != null) {
            house.setCurrentOccupancy(dto.getCurrentOccupancy());
        }

        house = poultryHouseRepository.save(house);
        log.info("Poultry house updated: {}", house.getId());

        return toDto(house);
    }

    /**
     * Delete poultry house (only if EMPTY)
     */
    public void deleteHouse(Long id) {
        log.info("Deleting poultry house {}", id);

        PoultryHouse house = poultryHouseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("House not found with id: " + id));

        // Can only delete EMPTY houses
        if (house.getStatus() != PoultryHouseStatus.EMPTY) {
            throw new IllegalStateException("Cannot delete house that is not EMPTY. Current status: " + house.getStatus());
        }

        // Check if house has current lot
        if (house.getCurrentLot() != null) {
            throw new IllegalStateException("Cannot delete house with active lot");
        }

        poultryHouseRepository.deleteById(id);
        log.info("Poultry house deleted: {}", id);
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Convert entity to DTO
     */
    private PoultryHouseDto toDto(PoultryHouse house) {
        PoultryHouseDto dto = new PoultryHouseDto();
        dto.setId(house.getId());
        dto.setFarmId(house.getFarm().getId());
        dto.setCapacity(house.getCapacity());
        dto.setArea(house.getArea());
        dto.setType(house.getType());
        dto.setEquipmentType(house.getEquipmentType());
        dto.setStatus(house.getStatus());
        dto.setCurrentOccupancy(house.getCurrentOccupancy());

        // Current lot info
        if (house.getCurrentLot() != null) {
            dto.setCurrentLot(house.getCurrentLot().getLotNumber());
        }

        return dto;
    }

    /**
     * Validate house DTO
     */
    private void validateHouseDto(PoultryHouseDto dto) {
        if (dto.getCapacity() == null || dto.getCapacity() <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0");
        }
        if (dto.getFarmId() == null) {
            throw new IllegalArgumentException("Farm ID is required");
        }
        if (dto.getType() == null) {
            throw new IllegalArgumentException("House type is required");
        }
    }
}
package com.example.broilerfarm.application.rest;

import com.example.broilerfarm.application.dto.PoultryHouseDto;
import com.example.broilerfarm.services.PoultryHouseTransformationService;
import com.example.broilerfarm.domain.entities.PoultryHouse;
import com.example.broilerfarm.infrastructure.persistence.repositories.PoultryHouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/poultry-houses")
@CrossOrigin(origins = "*")
public class PoultryHouseRESTService {

    @Autowired
    private PoultryHouseRepository poultryHouseRepository;

    @Autowired
    private PoultryHouseTransformationService transformationService;

    /**
     * CREATE
     */
    @PostMapping
    public ResponseEntity<PoultryHouseDto> create(@RequestBody PoultryHouseDto houseDto) {
        try {
            if (houseDto.getFarmId() == null) {
                return ResponseEntity.badRequest().build();
            }

            PoultryHouse house = transformationService.toEntity(houseDto);
            if (house.getFarm() == null) {
                return ResponseEntity.badRequest().build();
            }

            house = poultryHouseRepository.save(house);
            return ResponseEntity.status(HttpStatus.CREATED).body(transformationService.toDto(house));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PoultryHouseDto> getById(@PathVariable Long id) {
        try {
            PoultryHouse house = poultryHouseRepository.findById(id).orElse(null);
            if (house == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(transformationService.toDto(house));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get all
     */
    @GetMapping
    public ResponseEntity<List<PoultryHouseDto>> getAll() {
        try {
            List<PoultryHouse> houses = poultryHouseRepository.findAll();
            List<PoultryHouseDto> dtos = houses.stream()
                    .map(transformationService::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get by Farm ID
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<PoultryHouseDto>> getByFarmId(@PathVariable Long farmId) {
        try {
            List<PoultryHouse> houses = poultryHouseRepository.findByFarmId(farmId);
            List<PoultryHouseDto> dtos = houses.stream()
                    .map(transformationService::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * UPDATE
     */
    @PutMapping("/{id}")
    public ResponseEntity<PoultryHouseDto> update(@PathVariable Long id, @RequestBody PoultryHouseDto houseDto) {
        try {
            PoultryHouse house = poultryHouseRepository.findById(id).orElse(null);
            if (house == null) {
                return ResponseEntity.notFound().build();
            }

            if (houseDto.getCapacity() != null) {
                house.setCapacity(houseDto.getCapacity());
            }
            if (houseDto.getCurrentLot() != null) {
                house.setCurrentLot(houseDto.getCurrentLot());
            }
            if (houseDto.getArea() != null) {
                house.setArea(houseDto.getArea());
            }
            if (houseDto.getType() != null) {
                house.setType(houseDto.getType());
            }
            if (houseDto.getEquipmentType() != null) {
                house.setEquipmentType(houseDto.getEquipmentType());
            }
            if (houseDto.getStatus() != null) {
                house.setStatus(houseDto.getStatus());
            }
            if (houseDto.getCurrentOccupancy() != null) {
                house.setCurrentOccupancy(houseDto.getCurrentOccupancy());
            }

            house = poultryHouseRepository.save(house);
            return ResponseEntity.ok(transformationService.toDto(house));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            PoultryHouse house = poultryHouseRepository.findById(id).orElse(null);
            if (house == null) {
                return ResponseEntity.notFound().build();
            }

            poultryHouseRepository.delete(house);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
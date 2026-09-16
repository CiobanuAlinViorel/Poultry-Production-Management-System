package com.example.broilerfarm.application.rest;
import com.example.broilerfarm.application.transformation.BroilerFarmTransformationService;
import com.example.broilerfarm.application.dto.BroilerFarmDto;
import com.example.broilerfarm.domain.entities.BroilerFarm;
import com.example.broilerfarm.infrastructure.persistence.repositories.BroilerFarmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/broiler-farms")
@CrossOrigin(origins = "*")
public class BroilerFarmRESTService {

    @Autowired
    private BroilerFarmRepository broilerFarmRepository;

    @Autowired
    private BroilerFarmTransformationService transformationService;

    /**
     * CREATE
     */
    @PostMapping
    public ResponseEntity<BroilerFarmDto> create(@RequestBody BroilerFarmDto farmDto) {
        try {
            if (farmDto.getLicenseNumber() == null || farmDto.getLicenseNumber().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            BroilerFarm farm = transformationService.toEntity(farmDto);
            farm = broilerFarmRepository.save(farm);

            return ResponseEntity.status(HttpStatus.CREATED).body(transformationService.toDto(farm));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BroilerFarmDto> getById(@PathVariable Long id) {
        try {
            BroilerFarm farm = broilerFarmRepository.findById(id).orElse(null);
            if (farm == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(transformationService.toDto(farm));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get all
     */
    @GetMapping
    public ResponseEntity<List<BroilerFarmDto>> getAll() {
        try {
            List<BroilerFarm> farms = broilerFarmRepository.findAll();
            List<BroilerFarmDto> dtos = farms.stream()
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
    public ResponseEntity<BroilerFarmDto> update(@PathVariable Long id, @RequestBody BroilerFarmDto farmDto) {
        try {
            BroilerFarm farm = broilerFarmRepository.findById(id).orElse(null);
            if (farm == null) {
                return ResponseEntity.notFound().build();
            }

            if (farmDto.getFarmName() != null) {
                farm.setFarmName(farmDto.getFarmName());
            }
            if (farmDto.getLocation() != null) {
                farm.setLocation(farmDto.getLocation());
            }
            if (farmDto.getAddress() != null) {
                farm.setAddress(farmDto.getAddress());
            }
            if (farmDto.getCapacity() != null) {
                farm.setCapacity(farmDto.getCapacity());
            }
            if (farmDto.getLicenseNumber() != null) {
                farm.setLicenseNumber(farmDto.getLicenseNumber());
            }

            farm = broilerFarmRepository.save(farm);
            return ResponseEntity.ok(transformationService.toDto(farm));
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
            BroilerFarm farm = broilerFarmRepository.findById(id).orElse(null);
            if (farm == null) {
                return ResponseEntity.notFound().build();
            }

            broilerFarmRepository.delete(farm);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
package com.example.slaughterhouse.application.rest;

import com.example.slaughterhouse.application.dto.SlaughterLotDto;
import com.example.slaughterhouse.application.transformation.SlaughterLotTransformationService;
import com.example.slaughterhouse.domain.entities.SlaughterLot;
import com.example.slaughterhouse.domain.enums.LotStatus;
import com.example.slaughterhouse.infrastructure.persistance.repository.SlaughterLotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/slaughter-lots")
@CrossOrigin(origins = "*")
public class SlaughterLotRESTController {

    @Autowired
    private SlaughterLotRepository slaughterLotRepository;

    @Autowired
    private SlaughterLotTransformationService transformationService;

    /**
     * CREATE - Create a new slaughter lot
     */
    @PostMapping
    public ResponseEntity<SlaughterLotDto> create(@RequestBody SlaughterLotDto lotDto) {
        try {
            SlaughterLot lot = transformationService.toEntity(lotDto);
            lot = slaughterLotRepository.save(lot);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(transformationService.toDto(lot));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get lot by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<SlaughterLotDto> getById(@PathVariable Long id) {
        try {
            SlaughterLot lot = slaughterLotRepository.findById(id).orElse(null);
            if (lot == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(transformationService.toDto(lot));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get lot by lot number
     */
    @GetMapping("/lot-number/{lotNumber}")
    public ResponseEntity<SlaughterLotDto> getByLotNumber(@PathVariable String lotNumber) {
        try {
            SlaughterLot lot = slaughterLotRepository.findByLotNumber(lotNumber).orElse(null);
            if (lot == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(transformationService.toDto(lot));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get all lots
     */
    @GetMapping
    public ResponseEntity<List<SlaughterLotDto>> getAll() {
        try {
            List<SlaughterLot> lots = slaughterLotRepository.findByIsActiveTrue();
            List<SlaughterLotDto> dtos = lots.stream()
                    .map(transformationService::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get lots by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<SlaughterLotDto>> getByStatus(@PathVariable LotStatus status) {
        try {
            List<SlaughterLot> lots = slaughterLotRepository.findByStatus(status);
            List<SlaughterLotDto> dtos = lots.stream()
                    .map(transformationService::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get active lots
     */
    @GetMapping("/active")
    public ResponseEntity<List<SlaughterLotDto>> getActive() {
        try {
            List<SlaughterLot> lots = slaughterLotRepository.findByIsActiveTrue();
            List<SlaughterLotDto> dtos = lots.stream()
                    .map(transformationService::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * UPDATE - Update slaughter lot
     */
    @PutMapping("/{id}")
    public ResponseEntity<SlaughterLotDto> update(@PathVariable Long id, @RequestBody SlaughterLotDto lotDto) {
        try {
            SlaughterLot lot = slaughterLotRepository.findById(id).orElse(null);
            if (lot == null) {
                return ResponseEntity.notFound().build();
            }

            // Update fields
            if (lotDto.getBreed() != null) {
                lot.setBreed(lotDto.getBreed());
            }
            if (lotDto.getSlaughterDate() != null) {
                lot.setSlaughterDate(lotDto.getSlaughterDate());
            }
            if (lotDto.getCurrentQuantity() != null) {
                lot.setCurrentQuantity(lotDto.getCurrentQuantity());
            }
            if (lotDto.getStatus() != null) {
                lot.setStatus(lotDto.getStatus());
            }

            lot = slaughterLotRepository.save(lot);
            return ResponseEntity.ok(transformationService.toDto(lot));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE - Delete lot (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            SlaughterLot lot = slaughterLotRepository.findById(id).orElse(null);
            if (lot == null) {
                return ResponseEntity.notFound().build();
            }

            lot.setIsActive(false);
            slaughterLotRepository.save(lot);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * BUSINESS LOGIC - Update current quantity
     */
    @PatchMapping("/{id}/quantity")
    public ResponseEntity<SlaughterLotDto> updateQuantity(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        try {
            SlaughterLot lot = slaughterLotRepository.findById(id).orElse(null);
            if (lot == null) {
                return ResponseEntity.notFound().build();
            }

            lot.updateCurrentQuantity(quantity);
            lot = slaughterLotRepository.save(lot);
            return ResponseEntity.ok(transformationService.toDto(lot));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * BUSINESS LOGIC - Calculate total weight
     */
    @PostMapping("/{id}/calculate-weight")
    public ResponseEntity<SlaughterLotDto> calculateTotalWeight(@PathVariable Long id) {
        try {
            SlaughterLot lot = slaughterLotRepository.findById(id).orElse(null);
            if (lot == null) {
                return ResponseEntity.notFound().build();
            }

            lot.calculateTotalWeight();
            lot = slaughterLotRepository.save(lot);
            return ResponseEntity.ok(transformationService.toDto(lot));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * BUSINESS LOGIC - Get mortality statistics
     */
    @GetMapping("/{id}/mortality-stats")
    public ResponseEntity<Map<String, Object>> getMortalityStats(@PathVariable Long id) {
        try {
            SlaughterLot lot = slaughterLotRepository.findById(id).orElse(null);
            if (lot == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("total_chickens", lot.getTotalChickens());
            stats.put("current_quantity", lot.getCurrentQuantity());
            stats.put("mortality_count", lot.calculateMortality());
            stats.put("mortality_rate", lot.calculateMortalityRate());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

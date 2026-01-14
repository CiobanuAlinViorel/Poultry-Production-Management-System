package com.example.slaughterhouse.application.rest;

import com.example.slaughterhouse.application.dto.ChickenReceptionDto;
import com.example.slaughterhouse.application.transformation.ChickenReceptionTransformationService;
import com.example.slaughterhouse.domain.entities.ChickenReception;
import com.example.slaughterhouse.infrastructure.persistance.repository.ChickenReceptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chicken-receptions")
@CrossOrigin(origins = "*")
public class ChickenReceptionRESTController {

    @Autowired
    private ChickenReceptionRepository receptionRepository;

    @Autowired
    private ChickenReceptionTransformationService transformationService;

    /**
     * CREATE - Create a new chicken reception
     */
    @PostMapping
    public ResponseEntity<ChickenReceptionDto> create(@RequestBody ChickenReceptionDto receptionDto) {
        try {
            if (receptionDto.getSlaughterLotId() == null || receptionDto.getReceivedById() == null) {
                return ResponseEntity.badRequest().build();
            }

            ChickenReception reception = transformationService.toEntity(receptionDto);
            if (reception.getSlaughterLot() == null || reception.getReceivedBy() == null) {
                return ResponseEntity.badRequest().build();
            }

            reception = receptionRepository.save(reception);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(transformationService.toDto(reception));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get reception by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChickenReceptionDto> getById(@PathVariable Long id) {
        try {
            ChickenReception reception = receptionRepository.findById(id).orElse(null);
            if (reception == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(transformationService.toDto(reception));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get all receptions
     */
    @GetMapping
    public ResponseEntity<List<ChickenReceptionDto>> getAll() {
        try {
            List<ChickenReception> receptions = receptionRepository.findByIsActiveTrue();
            List<ChickenReceptionDto> dtos = receptions.stream()
                    .map(transformationService::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get receptions by date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<ChickenReceptionDto>> getByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        try {
            List<ChickenReception> receptions = receptionRepository.findByReceptionDateBetween(startDate, endDate);
            List<ChickenReceptionDto> dtos = receptions.stream()
                    .map(transformationService::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get active receptions
     */
    @GetMapping("/active")
    public ResponseEntity<List<ChickenReceptionDto>> getActive() {
        try {
            List<ChickenReception> receptions = receptionRepository.findByIsActiveTrue();
            List<ChickenReceptionDto> dtos = receptions.stream()
                    .map(transformationService::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get receptions with high mortality
     */
    @GetMapping("/high-mortality")
    public ResponseEntity<List<ChickenReceptionDto>> getWithHighMortality() {
        try {
            List<ChickenReception> receptions = receptionRepository.findAll().stream()
                    .filter(ChickenReception::hasHighMortality)
                    .collect(Collectors.toList());
            List<ChickenReceptionDto> dtos = receptions.stream()
                    .map(transformationService::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * UPDATE - Update reception
     */
    @PutMapping("/{id}")
    public ResponseEntity<ChickenReceptionDto> update(
            @PathVariable Long id,
            @RequestBody ChickenReceptionDto receptionDto) {
        try {
            ChickenReception reception = receptionRepository.findById(id).orElse(null);
            if (reception == null) {
                return ResponseEntity.notFound().build();
            }

            // Update fields
            if (receptionDto.getReceptionDate() != null) {
                reception.setReceptionDate(receptionDto.getReceptionDate());
            }
            if (receptionDto.getReceptionTime() != null) {
                reception.setReceptionTime(receptionDto.getReceptionTime());
            }
            if (receptionDto.getQuantityReceived() != null) {
                reception.setQuantityReceived(receptionDto.getQuantityReceived());
            }
            if (receptionDto.getChicksAlive() != null) {
                reception.setChicksAlive(receptionDto.getChicksAlive());
            }
            if (receptionDto.getChicksDOA() != null) {
                reception.setChicksDOA(receptionDto.getChicksDOA());
            }
            if (receptionDto.getTransportConditions() != null) {
                reception.setTransportConditions(receptionDto.getTransportConditions());
            }
            if (receptionDto.getAnimalWelfareCheck() != null) {
                reception.setAnimalWelfareCheck(receptionDto.getAnimalWelfareCheck());
            }
            if (receptionDto.getAnimalWelfareNotes() != null) {
                reception.setAnimalWelfareNotes(receptionDto.getAnimalWelfareNotes());
            }
            if (receptionDto.getNotes() != null) {
                reception.setNotes(receptionDto.getNotes());
            }

            reception = receptionRepository.save(reception);
            return ResponseEntity.ok(transformationService.toDto(reception));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE - Delete reception (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            ChickenReception reception = receptionRepository.findById(id).orElse(null);
            if (reception == null) {
                return ResponseEntity.notFound().build();
            }

            reception.setIsActive(false);
            receptionRepository.save(reception);
            return ResponseEntity.noContent().build();
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
            ChickenReception reception = receptionRepository.findById(id).orElse(null);
            if (reception == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("quantity_received", reception.getQuantityReceived());
            stats.put("chicks_alive", reception.getChicksAlive());
            stats.put("chicks_doa", reception.getChicksDOA());
            stats.put("mortality_rate", reception.calculateMortalityRate());
            stats.put("has_high_mortality", reception.hasHighMortality());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

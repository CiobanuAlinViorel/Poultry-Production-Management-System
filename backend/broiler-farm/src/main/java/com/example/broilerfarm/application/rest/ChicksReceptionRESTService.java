package com.example.broilerfarm.application.rest;

import com.example.broilerfarm.application.ChicksReceptionApplicationService;
import com.example.broilerfarm.application.dto.ChickReceptionDto;
import com.example.broilerfarm.application.transformation.ChicksReceptionTransformationService;
import com.example.broilerfarm.domain.entities.ChicksReception;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for ChicksReception operations.
 * Handles HTTP requests for UC-01 (Create) and UC-02 (Update) workflows.
 */
@RestController
@RequestMapping("/api/chicks-receptions")
@CrossOrigin(origins = "*")
public class ChicksReceptionRESTService {

    @Autowired
    private ChicksReceptionApplicationService chicksReceptionApplicationService;

    @Autowired
    private ChicksReceptionTransformationService chicksReceptionTransformationService;

    /**
     * UC-01: CREATE new reception (DRAFT status)
     * POST /api/chicks-receptions
     */
    @PostMapping
    public ResponseEntity<ChickReceptionDto> create(@Valid @RequestBody ChickReceptionDto receptionDto) {
        try {
            // Validation
            if (receptionDto.getFarmId() == null) {
                return ResponseEntity.badRequest().body(null);
            }
            if (receptionDto.getEmployeeid() == null) {
                return ResponseEntity.badRequest().body(null);
            }
            if (receptionDto.getLines() == null || receptionDto.getLines().isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }

            // Create reception in DRAFT status
            ChicksReception reception = chicksReceptionApplicationService.createReception(receptionDto);
            ChickReceptionDto responseDto = chicksReceptionTransformationService.toDto(reception);

            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get reception by ID
     * GET /api/chicks-receptions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChickReceptionDto> getById(@PathVariable Long id) {
        try {
            ChickReceptionDto reception = chicksReceptionApplicationService.getReceptionById(id);
            return ResponseEntity.ok(reception);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get all receptions
     * GET /api/chicks-receptions
     */
    @GetMapping
    public ResponseEntity<List<ChickReceptionDto>> getAll() {
        try {
            List<ChickReceptionDto> dtos = chicksReceptionApplicationService.getAllReceptions();
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get receptions by farm ID
     * GET /api/chicks-receptions/farm/{farmId}
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<ChickReceptionDto>> getByFarmId(@PathVariable Long farmId) {
        try {
            List<ChickReceptionDto> dtos = chicksReceptionApplicationService.getReceptionsByFarm(farmId);
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * UC-02: UPDATE reception (only DRAFT status can be updated)
     * PUT /api/chicks-receptions/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ChickReceptionDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ChickReceptionDto receptionDto) {
        try {
            ChickReceptionDto updatedReception = chicksReceptionApplicationService.updateReception(
                    id,
                    receptionDto.getReceptionDate(),
                    receptionDto.getEmployeeid(),
                    receptionDto.getTransportConditions(),
                    receptionDto.getTruckInfo(),
                    receptionDto.getReferenceDocument(),
                    receptionDto.getLines()
            );
            return ResponseEntity.ok(updatedReception);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            // Reception is finalized, cannot update
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * FINALIZE reception - creates lots and updates house status
     * POST /api/chicks-receptions/{id}/finalize
     */
    @PostMapping("/{id}/finalize")
    public ResponseEntity<ChickReceptionDto> finalize(@PathVariable Long id) {
        try {
            ChickReceptionDto finalizedReception = chicksReceptionApplicationService.finalizeReception(id);
            return ResponseEntity.ok(finalizedReception);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            // Already finalized or validation failed
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE reception (only DRAFT can be deleted)
     * DELETE /api/chicks-receptions/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            chicksReceptionApplicationService.deleteReception(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            // Reception is finalized or has associated data
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
package com.example.broilerfarm.application.rest;

import com.example.broilerfarm.application.PoultryHouseApplicationService;
import com.example.broilerfarm.application.dto.PoultryHouseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Poultry House CRUD operations
 */
@RestController
@RequestMapping("/api/poultry-houses")
@RequiredArgsConstructor
@Slf4j
public class PoultryHouseRESTService {

    private final PoultryHouseApplicationService poultryHouseService;

    /**
     * CREATE - Add new poultry house
     * POST /api/poultry-houses
     */
    @PostMapping
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<PoultryHouseDto> create(@RequestBody PoultryHouseDto dto) {
        try {
            PoultryHouseDto created = poultryHouseService.createHouse(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating house", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get all poultry houses
     * GET /api/poultry-houses
     */
    @GetMapping
    public ResponseEntity<List<PoultryHouseDto>> getAll() {
        try {
            List<PoultryHouseDto> houses = poultryHouseService.getAllHouses();
            return ResponseEntity.ok(houses);
        } catch (Exception e) {
            log.error("Error fetching houses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get poultry house by ID
     * GET /api/poultry-houses/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PoultryHouseDto> getById(@PathVariable Long id) {
        try {
            PoultryHouseDto house = poultryHouseService.getHouseById(id);
            return ResponseEntity.ok(house);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching house", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * READ - Get poultry houses by farm
     * GET /api/poultry-houses/farm/{farmId}
     */
    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<PoultryHouseDto>> getByFarm(@PathVariable Long farmId) {
        try {
            List<PoultryHouseDto> houses = poultryHouseService.getHousesByFarm(farmId);
            return ResponseEntity.ok(houses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching houses by farm", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * UPDATE - Update poultry house
     * PUT /api/poultry-houses/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<PoultryHouseDto> update(
            @PathVariable Long id,
            @RequestBody PoultryHouseDto dto) {
        try {
            PoultryHouseDto updated = poultryHouseService.updateHouse(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating house", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE - Delete poultry house (only if EMPTY)
     * DELETE /api/poultry-houses/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            log.info("Deleting poultry house {}", id);
            poultryHouseService.deleteHouse(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            // House is occupied, cannot delete
            log.error("Cannot delete occupied house: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error deleting house", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
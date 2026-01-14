package com.example.broilerfarm.application;

import com.example.broilerfarm.application.dto.ChickReceptionDto;
import com.example.broilerfarm.application.dto.ChicksReceptionLineDto;
import com.example.broilerfarm.domain.entities.*;
import com.example.broilerfarm.domain.enums.ChicksLotStatus;
import com.example.broilerfarm.domain.enums.PoultryHouseStatus;
import com.example.broilerfarm.domain.enums.ReceptionStatus;
import com.example.broilerfarm.infrastructure.persistence.repositories.*;
import com.example.broilerfarm.application.transformation.ChicksReceptionTransformationService;
import com.example.broilerfarm.services.ChicksLotFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Application Service for managing Chicks Reception operations.
 * Implements UC-01 (Create Reception) and UC-02 (Edit Reception).
 */
@Service
public class ChicksReceptionApplicationService {

    @Autowired
    private ChicksReceptionRepository chicksReceptionRepository;

    @Autowired
    private PoultryHouseRepository poultryHouseRepository;

    @Autowired
    private BroilerFarmRepository broilerFarmRepository;

    @Autowired
    private ChicksLotFactory chicksLotFactory;

    @Autowired
    private FarmEmployeeRepository farmEmployeeRepository;

    @Autowired
    private ChicksReceptionTransformationService transformationService;

    @Autowired
    private ChicksLotRepository chicksLotRepository;

    /**
     * UC-01: Create a new Chicks Reception
     * Creates reception in DRAFT status without creating lots yet.
     * Lots are created when reception is finalized.
     */
    @Transactional
    public ChicksReception createReception(ChickReceptionDto receptionDto) {
        // Validate input
        validateReceptionDto(receptionDto);

        // Transform DTO to Entity
        ChicksReception reception = transformationService.toEntity(receptionDto);

        // Set initial status to DRAFT
        reception.setStatus(ReceptionStatus.DRAFT);

        // Validate business rules
        validateReceptionEntity(reception);

        // Add reception lines to reception
        List<ChicksReceptionLine> lines = new ArrayList<>();
        for (ChicksReceptionLineDto lineDto : receptionDto.getLines()) {
            ChicksReceptionLine line = transformationService.toLineEntity(lineDto);
            lines.add(line);
            reception.addReceptionLine(line);
        }

        // Save reception (still in DRAFT)

        return chicksReceptionRepository.save(reception);
    }

    /**
     * UC-02: Update an existing reception.
     * Can only update receptions in DRAFT status.
     */
    @Transactional
    public ChickReceptionDto updateReception(
            Long receptionId,
            LocalDateTime receptionDate,
            Long employeeId,
            String transportConditions,
            String truckInfo,
            String referenceDocument,
            List<ChicksReceptionLineDto> lines
    ) {
        if (receptionId == null) {
            throw new IllegalArgumentException("Reception ID cannot be null");
        }

        ChicksReception reception = chicksReceptionRepository.findById(receptionId)
                .orElseThrow(() -> new IllegalArgumentException("Reception not found"));

        // Can only edit DRAFT receptions
        if (reception.getStatus() != ReceptionStatus.DRAFT) {
            throw new IllegalStateException("Cannot update finalized reception");
        }

        // Update header fields
        if (receptionDate != null) {
            reception.setReceptionDate(receptionDate);
        }

        if (employeeId != null) {
            FarmEmployee employee = farmEmployeeRepository.findById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
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

        // Update reception lines if provided
        if (lines != null && !lines.isEmpty()) {
            updateReceptionLines(reception, lines);
        }

        reception = chicksReceptionRepository.save(reception);
        return transformationService.toDto(reception);
    }

    /**
     * Helper method to update reception lines.
     * Handles add, update, and remove operations.
     */
    private void updateReceptionLines(ChicksReception reception, List<ChicksReceptionLineDto> lineDtos) {
        List<ChicksReceptionLine> existingLines = new ArrayList<>(reception.getReceptionLines());
        List<ChicksReceptionLine> newLines = lineDtos.stream()
                .map(transformationService::toLineEntity)
                .toList();

        // Remove lines that are no longer in the update
        for (ChicksReceptionLine existingLine : existingLines) {
            boolean found = newLines.stream()
                    .anyMatch(newLine -> newLine.getId() != null &&
                            newLine.getId().equals(existingLine.getId()));
            if (!found) {
                reception.removeReceptionLine(existingLine.getId());
            }
        }

        // Add or update lines
        for (ChicksReceptionLine newLine : newLines) {
            if (newLine.getId() == null) {
                // New line - add it
                reception.addReceptionLine(newLine);
            } else {
                // Existing line - update it
                existingLines.stream()
                        .filter(line -> line.getId().equals(newLine.getId()))
                        .findFirst().ifPresent(existingLine -> reception.updateReceptionLine(
                                existingLine.getId(),
                                newLine.getChicksAlive(),
                                newLine.getChicksDOA(),
                                newLine.getChicksWeak(),
                                newLine.getQualityGrade(),
                                newLine.getNotes(),
                                LocalDateTime.now()
                        ));

            }
        }
    }

    /**
     * Finalize a reception and create lots for each line.
     * This is the actual UC-01 step 7-9 implementation.
     */
    @Transactional
    public ChickReceptionDto finalizeReception(Long receptionId) {
        if (receptionId == null) {
            throw new IllegalArgumentException("Reception ID cannot be null");
        }

        ChicksReception reception = chicksReceptionRepository.findById(receptionId)
                .orElseThrow(() -> new IllegalArgumentException("Reception not found"));

        if (reception.getStatus() != ReceptionStatus.DRAFT) {
            throw new IllegalStateException("Can only finalize DRAFT reception");
        }

        if (reception.getReceptionLines().isEmpty()) {
            throw new IllegalStateException("Cannot finalize reception without lines");
        }

        // Validate all lines have poultry houses assigned
        for (ChicksReceptionLine line : reception.getReceptionLines()) {
            if (line.getPoultryHouse() == null) {
                throw new IllegalStateException("All lines must have poultry house assigned");
            }
        }

        // Create lots for each line and update poultry houses
        for (ChicksReceptionLine line : reception.getReceptionLines()) {
            if (line.getCreatedLot() == null) {
                PoultryHouse house = line.getPoultryHouse();

                // Create lot using factory
                ChicksLot lot = chicksLotFactory.createLot(line);

                // Save lot and get managed instance
                ChicksLot savedLot = chicksLotRepository.save(lot);

                // Set created lot on line
                line.setCreatedLot(savedLot);

                // ✅ UPDATE POULTRY HOUSE:
                // 1. Set current lot
                house.setCurrentLot(savedLot);

                // 2. Set status to OCCUPIED
                house.setStatus(PoultryHouseStatus.OCCUPIED);

                // 3. ✅ UPDATE CURRENT OCCUPANCY - total chicks alive in this house
                house.setCurrentOccupancy(line.getChicksAlive());

                // 4. Save house to persist all changes
                poultryHouseRepository.save(house);

            }
        }

        // Finalize reception (changes status to FINALIZED)
        reception.finalizeReception();
        reception = chicksReceptionRepository.save(reception);



        return transformationService.toDto(reception);
    }

    /**
     * Get all receptions.
     */
    @Transactional(readOnly = true)
    public List<ChickReceptionDto> getAllReceptions() {
        List<ChicksReception> receptions = chicksReceptionRepository.findAll();
        return receptions.stream()
                .map(transformationService::toDto)
                .toList();
    }

    /**
     * Get reception by ID.
     */
    @Transactional(readOnly = true)
    public ChickReceptionDto getReceptionById(Long receptionId) {
        if (receptionId == null) {
            throw new IllegalArgumentException("Reception ID cannot be null");
        }

        ChicksReception reception = chicksReceptionRepository.findById(receptionId)
                .orElseThrow(() -> new IllegalArgumentException("Reception not found"));

        return transformationService.toDto(reception);
    }

    /**
     * Get all receptions for a specific farm.
     */
    @Transactional(readOnly = true)
    public List<ChickReceptionDto> getReceptionsByFarm(Long farmId) {
        if (farmId == null) {
            throw new IllegalArgumentException("Farm ID cannot be null");
        }

        BroilerFarm farm = broilerFarmRepository.findById(farmId)
                .orElseThrow(() -> new IllegalArgumentException("Farm not found"));

        List<ChicksReception> receptions = chicksReceptionRepository.findByFarmId(farm.getId());
        return receptions.stream()
                .map(transformationService::toDto)
                .toList();
    }

    /**
     * Delete a reception.
     * Can only delete DRAFT receptions or receptions without associated lots.
     */
    @Transactional
    public void deleteReception(Long receptionId) {
        if (receptionId == null) {
            throw new IllegalArgumentException("Reception ID cannot be null");
        }

        ChicksReception reception = chicksReceptionRepository.findById(receptionId)
                .orElseThrow(() -> new IllegalArgumentException("Reception not found"));

        // Can only delete DRAFT receptions
//        if (reception.getStatus() != ReceptionStatus.DRAFT) {
//            throw new IllegalStateException("Cannot delete finalized reception");
//        }

        // Remove all lines and associated lots
        List<ChicksReceptionLine> lines = new ArrayList<>(reception.getReceptionLines());
        for (ChicksReceptionLine line : lines) {
            ChicksLot lot = line.getCreatedLot();
            if (lot != null) {
                // Check if lot has any associated data
                if (hasAssociatedData(lot)) {
                    throw new IllegalStateException(
                            "Cannot delete reception with lots that have mortality, consumption, or treatment records");
                }

                // ✅ RESET POULTRY HOUSE when deleting reception
                PoultryHouse house = line.getPoultryHouse();
                if (house != null && house.getCurrentLot() != null &&
                        house.getCurrentLot().getLotNumber().equals(lot.getLotNumber())) {

                    // 1. Remove current lot
                    house.setCurrentLot(null);

                    // 2. Set status to EMPTY
                    house.setStatus(PoultryHouseStatus.EMPTY);

                    // 3. ✅ RESET CURRENT OCCUPANCY to 0
                    house.setCurrentOccupancy(0);

                    // 4. Save house
                    poultryHouseRepository.save(house);


                }

                // Delete the lot
                chicksLotRepository.delete(lot);
            }

            // Remove line from reception
            reception.removeReceptionLine(line.getId());
        }

        // Delete the reception
        chicksReceptionRepository.deleteById(receptionId);
    }

    /**
     * Check if a lot has associated data (mortality, consumption, treatments).
     */
    private boolean hasAssociatedData(ChicksLot lot) {
        // Since ChicksLot doesn't have these collections, we need to check via repositories
        // This is a simplified check - in production you'd query repositories
        return false; // Placeholder - implement based on your repository queries
    }

    /**
     * Validate reception DTO before transformation.
     */
    private void validateReceptionDto(ChickReceptionDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Reception DTO cannot be null");
        }
        if (dto.getLines() == null || dto.getLines().isEmpty()) {
            throw new IllegalArgumentException("Reception must have at least one line");
        }
    }

    /**
     * Validate reception entity business rules.
     */
    private void validateReceptionEntity(ChicksReception reception) {
        if (reception.getFarm() == null) {
            throw new IllegalArgumentException("Farm cannot be null");
        }
        if (reception.getReceivingEmployee() == null) {
            throw new IllegalArgumentException("Receiving employee cannot be null");
        }
        if (reception.getReceptionDate() == null) {
            throw new IllegalArgumentException("Reception date cannot be null");
        }
    }
}
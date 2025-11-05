package com.example.broilerfarm.domain.services;

import com.example.broilerfarm.domain.entities.ChicksLot;
import com.example.broilerfarm.domain.entities.ChicksReceptionLine;
import com.example.broilerfarm.domain.entities.PoultryHouse;
import com.example.broilerfarm.domain.enums.ChicksLotStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Factory pentru crearea ChicksLot cu business rules corecte
 * UC-01: Creating the Chicks Reception
 */
@Service
public class ChicksLotFactory {

    /**
     * Creează un ChicksLot din ChicksReceptionLine
     *
     * @param line Linia de recepție cu datele puilor
     * @param breed Rasa puilor
     * @param hatcherySource Sursa incubatorului
     * @return ChicksLot creat și validat
     */
    public ChicksLot createLot(ChicksReceptionLine line, String breed, String hatcherySource) {
        validateReceptionLine(line);

        PoultryHouse house = line.getPoultryHouse();
        validateHouse(house, line.getChicksAlive());

        String lotNumber = generateLotNumber(house, line.getReception().getReceptionDate());

        return ChicksLot.builder()
                .lotNumber(lotNumber)
                .house(house)
                .breed(breed)
                .hatcherySource(hatcherySource)
                .receptionDate(line.getReception().getReceptionDate().toLocalDate())
                .initialQuantity(line.getChicksAlive())
                .currentQuantity(line.getChicksAlive())
                .status(ChicksLotStatus.GROWING)
                .expectedMortalityRate(calculateExpectedMortalityRate(breed))
                .build();
    }

    /**
     * Generează număr unic de lot: FARM-HOUSE-YYYY-MM-DD
     */
    private String generateLotNumber(PoultryHouse house, java.time.LocalDateTime receptionDate) {
        Long farmId = house.getFarm().getId();
        Long houseId = house.getId();
        String date = receptionDate.toLocalDate().toString();

        return String.format("%d-%d-%s", farmId, houseId, date);
    }

    /**
     * Validare ChicksReceptionLine
     */
    private void validateReceptionLine(ChicksReceptionLine line) {
        if (line == null) {
            throw new IllegalArgumentException("Reception line cannot be null");
        }

        if (line.getPoultryHouse() == null) {
            throw new IllegalStateException("Poultry house must be assigned before creating lot");
        }

        if (line.getChicksAlive() <= 0) {
            throw new IllegalArgumentException("Chicks alive must be positive");
        }

        line.validateQuantities(); // Folosește validarea din entitate
    }

    /**
     * Validare PoultryHouse
     */
    private void validateHouse(PoultryHouse house, Integer chicksAlive) {
        if (house.getStatus() != com.example.broilerfarm.domain.enums.PoultryHouseStatus.EMPTY) {
            throw new IllegalStateException(
                    "House " + house.getId() + " is not EMPTY. Current status: " + house.getStatus()
            );
        }

        if (chicksAlive > house.getCapacity()) {
            throw new IllegalStateException(
                    String.format(
                            "Chicks quantity (%d) exceeds house capacity (%d)",
                            chicksAlive,
                            house.getCapacity()
                    )
            );
        }
    }

    /**
     * Calculează rata așteptată de mortalitate bazată pe rasă
     */
    private double calculateExpectedMortalityRate(String breed) {
        // Valori tipice pentru diferite rase
        return switch (breed.toUpperCase()) {
            case "ROSS 308" -> 4.5;
            case "COBB 500" -> 4.0;
            case "ARBOR ACRES" -> 4.2;
            default -> 5.0; // Default conservativ
        };
    }

    /**
     * Calculează data estimată pentru tăiere (35-42 zile)
     */
    public LocalDate calculateExpectedSlaughterDate(LocalDate receptionDate) {
        return receptionDate.plusDays(38); // Standard 38 zile
    }
}
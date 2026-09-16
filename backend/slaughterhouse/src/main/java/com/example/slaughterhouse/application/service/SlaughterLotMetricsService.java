package com.example.slaughterhouse.application.service;

import com.example.slaughterhouse.domain.entities.SlaughterLot;
import com.example.slaughterhouse.infrastructure.persistance.repository.SlaughterLotRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SlaughterLotMetricsService {

    private final SlaughterLotRepository lotRepository;

    public SlaughterLotMetricsService(SlaughterLotRepository lotRepository) {
        this.lotRepository = lotRepository;
    }

    public SlaughterLotMetrics calculateMetrics(Long lotId) {
        Optional<SlaughterLot> lotOpt = lotRepository.findById(lotId);
        if (lotOpt.isEmpty()) {
            throw new IllegalArgumentException("Lot not found with id: " + lotId);
        }

        SlaughterLot lot = lotOpt.get();
        lot.calculateTotalWeight(); // actualizează totalWeight pe baza averageWeight și currentQuantity

        int mortality = lot.calculateMortality();
        float mortalityRate = lot.calculateMortalityRate();
        Float totalWeightValue = lot.getTotalWeight() != null ? lot.getTotalWeight().getValue() : null;

        return new SlaughterLotMetrics(totalWeightValue, mortality, mortalityRate);
    }

    public record SlaughterLotMetrics(Float totalWeight, int mortality, float mortalityRate) {}
}

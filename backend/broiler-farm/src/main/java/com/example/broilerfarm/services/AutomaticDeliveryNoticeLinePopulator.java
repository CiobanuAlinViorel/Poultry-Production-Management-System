package com.example.broilerfarm.services;

import com.example.broilerfarm.domain.entities.DeliveryNoticeLine;
import com.example.broilerfarm.domain.entities.ObservationSheet;
import com.example.broilerfarm.domain.enums.QualityGrade;

import java.math.BigDecimal;

public class AutomaticDeliveryNoticeLinePopulator {


    // ✅ Populate from ObservationSheet (Option A - UC-10)
    public DeliveryNoticeLine populateLineFromObservationSheet(ObservationSheet observation) {
        if (observation == null) {
            throw new IllegalArgumentException("ObservationSheet cannot be null");
        }

        DeliveryNoticeLine line =  new DeliveryNoticeLine();

        line.setLot(observation.getLot());
        line.setEstimatedQuantity(observation.getEndingBirdCount());
        line.setAverageWeight(observation.getAverageWeight());

        // Determine quality grade based on performance
        if (observation.getFcr().compareTo(BigDecimal.valueOf(1.6)) <= 0) {
           line.setQualityGrade(QualityGrade.A);
        } else if (observation.getFcr().compareTo(BigDecimal.valueOf(1.9)) <= 0) {
            line.setQualityGrade(QualityGrade.B);
        } else {
            line.setQualityGrade(QualityGrade.C);
        }

        return line;
    }
}

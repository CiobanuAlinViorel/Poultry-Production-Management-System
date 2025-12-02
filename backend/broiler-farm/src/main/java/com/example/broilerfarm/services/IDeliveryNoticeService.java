package com.example.broilerfarm.services;

import com.example.broilerfarm.domain.entities.ChicksLot;
import com.example.broilerfarm.domain.entities.DeliveryNotice;
import com.example.broilerfarm.domain.enums.QualityGrade;

import java.math.BigDecimal;

public interface IDeliveryNoticeService {
    public void addLine(
            Long deliveryNoticeId,
            ChicksLot lot,
            Integer estimatedQuantity,
            BigDecimal averageWeight,
            QualityGrade qualityGrade,
            String specialInstructions,
            String loadingBay,
            Integer actualQuantityDelivered,
            BigDecimal actualAverageWeight
    );


    void updateLine(Long id, Integer estimatedQuantity, BigDecimal averageWeight, QualityGrade qualityGrade, String specialInstructions, String loadingBay, Integer actualQuantityDelivered, BigDecimal actualAverageWeight);

    public void removeLine(Long id);
}

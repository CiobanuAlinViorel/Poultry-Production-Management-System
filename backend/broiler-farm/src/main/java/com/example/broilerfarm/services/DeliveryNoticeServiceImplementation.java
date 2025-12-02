package com.example.broilerfarm.services;

import com.example.broilerfarm.domain.entities.ChicksLot;
import com.example.broilerfarm.domain.entities.DeliveryNotice;
import com.example.broilerfarm.domain.entities.DeliveryNoticeLine;
import com.example.broilerfarm.domain.enums.QualityGrade;
import com.example.broilerfarm.infrastructure.persistence.repositories.ChicksLotRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.DeliveryNoticeLineRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.DeliveryNoticeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class DeliveryNoticeServiceImplementation implements IDeliveryNoticeService {

    @Autowired
    private DeliveryNoticeRepository deliveryNoticeRepository;

    @Autowired
    private DeliveryNoticeLineRepository deliveryNoticeLineRepository;

    @Autowired
    private ChicksLotRepository chicksLotRepository;

    @Override
    public void addLine(Long deliveryNoticeId, ChicksLot lot, Integer estimatedQuantity, BigDecimal averageWeight, QualityGrade qualityGrade, String specialInstructions, String loadingBay, Integer actualQuantityDelivered, BigDecimal actualAverageWeight) {
        if(deliveryNoticeId == null){
            throw new IllegalArgumentException("DeliveryNotice is null");
        }

        if(lot == null){
            throw new IllegalArgumentException("Lot is null");
        }

        verifyData(estimatedQuantity, averageWeight, qualityGrade, specialInstructions, loadingBay, actualQuantityDelivered, actualAverageWeight);

        DeliveryNotice notice = deliveryNoticeRepository.findById(deliveryNoticeId).orElse(null);
        if(notice == null){
            throw new IllegalArgumentException("DeliveryNotice not found");
        }

        DeliveryNoticeLine line = DeliveryNoticeLine.builder()
                .deliveryNotice(notice)
                .lot(lot)
                .estimatedQuantity(estimatedQuantity)
                .averageWeight(averageWeight)
                .qualityGrade(qualityGrade)
                .specialInstructions(specialInstructions)
                .loadingBay(loadingBay)
                .actualQuantityDelivered(actualQuantityDelivered)
                .actualAverageWeight(actualAverageWeight)
                .build();

        notice.addDeliveryLine(line);
        deliveryNoticeRepository.save(notice);
    }

    @Override
    public void updateLine(Long id, Integer estimatedQuantity, BigDecimal averageWeight, QualityGrade qualityGrade, String specialInstructions, String loadingBay, Integer actualQuantityDelivered, BigDecimal actualAverageWeight) {
        if(id == null){
            throw new IllegalArgumentException("Id is null");
        }

        verifyData(estimatedQuantity, averageWeight, qualityGrade, specialInstructions, loadingBay, actualQuantityDelivered, actualAverageWeight);

        DeliveryNoticeLine line = deliveryNoticeLineRepository.findById(id).orElse(null);
        if(line == null){
            throw new IllegalArgumentException("DeliveryNoticeLine not found");
        }

        line.setEstimatedQuantity(estimatedQuantity);
        line.setAverageWeight(averageWeight);
        line.setQualityGrade(qualityGrade);
        line.setSpecialInstructions(specialInstructions);
        line.setLoadingBay(loadingBay);
        line.setActualQuantityDelivered(actualQuantityDelivered);
        line.setActualAverageWeight(actualAverageWeight);

        deliveryNoticeRepository.save(line.getDeliveryNotice());
    }


    @Override
    public void removeLine(Long id) {
        if(id == null){
            throw new IllegalArgumentException("Id is null");
        }

        DeliveryNoticeLine line = deliveryNoticeLineRepository.findById(id).orElse(null);
        if(line == null){
            throw new IllegalArgumentException("DeliveryNoticeLine not found");
        }

        DeliveryNotice notice = line.getDeliveryNotice();
        notice.removeDeliveryLine(line);
        deliveryNoticeLineRepository.delete(line);
        deliveryNoticeRepository.save(notice);
    }

    private void verifyData(
            Integer estimatedQuantity,
            BigDecimal averageWeight,
            QualityGrade qualityGrade,
            String specialInstructions,
            String loadingBay,
            Integer actualQuantityDelivered,
            BigDecimal actualAverageWeight
    ){
        if(estimatedQuantity == null || estimatedQuantity <= 0){
            throw new IllegalArgumentException("Estimated quantity cannot be null or negative or 0");
        }

        if(averageWeight == null || averageWeight.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Average weight cannot be null or negative or 0");
        }

        if(qualityGrade == null){
            throw new IllegalArgumentException("Quality grade cannot be null");
        }

        if(specialInstructions == null || specialInstructions.isEmpty()){
            throw new IllegalArgumentException("Special instructions cannot be null or empty");
        }

        if(loadingBay == null || loadingBay.isEmpty()){
            throw new IllegalArgumentException("Loading bay cannot be null or empty");
        }

        if(actualQuantityDelivered == null || actualQuantityDelivered <= 0){
            throw new IllegalArgumentException("Actual quantity delivered cannot be null or negative or 0");
        }

        if(actualAverageWeight == null || actualAverageWeight.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Actual average weight cannot be null or negative or 0");
        }
    }
}
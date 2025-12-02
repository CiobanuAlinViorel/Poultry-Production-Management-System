package com.example.broilerfarm.application.dto;
import com.example.broilerfarm.domain.enums.QualityGrade;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryNoticeLineDto {
    private Long id;
    private Long deliveryNoticeId;
    private Long lotId;
    private Integer estimatedQuantity;
    private BigDecimal averageWeight;
    private QualityGrade qualityGrade;
    private String specialInstructions;
    private String loadingBay;
    private Integer actualQuantityDelivered;
    private BigDecimal actualAverageWeight;
}

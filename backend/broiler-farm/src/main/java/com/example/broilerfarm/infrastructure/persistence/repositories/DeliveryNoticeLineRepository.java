package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.DeliveryNoticeLine;
import com.example.broilerfarm.domain.enums.QualityGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryNoticeLineRepository extends JpaRepository<DeliveryNoticeLine, Long> {

    List<DeliveryNoticeLine> findByDeliveryNoticeId(Long deliveryNoticeId);

    Optional<DeliveryNoticeLine> findByLotId(Long lotId);

    @Query("SELECT dnl FROM DeliveryNoticeLine dnl WHERE dnl.deliveryNotice.id = :deliveryNoticeId " +
            "AND dnl.lot.id = :lotId")
    Optional<DeliveryNoticeLine> findByDeliveryNoticeIdAndLotId(
            @Param("deliveryNoticeId") Long deliveryNoticeId,
            @Param("lotId") Long lotId
    );

    List<DeliveryNoticeLine> findByQualityGrade(QualityGrade grade);

    @Query("SELECT dnl FROM DeliveryNoticeLine dnl WHERE dnl.actualQuantityDelivered IS NOT NULL")
    List<DeliveryNoticeLine> findLinesWithActualData();

    @Query("SELECT dnl FROM DeliveryNoticeLine dnl WHERE dnl.deliveryNotice.farm.id = :farmId")
    List<DeliveryNoticeLine> findByFarmId(@Param("farmId") Long farmId);

    @Query("SELECT SUM(dnl.estimatedQuantity) FROM DeliveryNoticeLine dnl " +
            "WHERE dnl.deliveryNotice.id = :deliveryNoticeId")
    Integer getTotalEstimatedQuantityByDeliveryNotice(@Param("deliveryNoticeId") Long deliveryNoticeId);

    @Query("SELECT AVG(dnl.averageWeight) FROM DeliveryNoticeLine dnl " +
            "WHERE dnl.deliveryNotice.id = :deliveryNoticeId")
    java.math.BigDecimal getAverageWeightByDeliveryNotice(@Param("deliveryNoticeId") Long deliveryNoticeId);

    // ✅ Variance analysis
    @Query("SELECT dnl FROM DeliveryNoticeLine dnl WHERE dnl.actualQuantityDelivered IS NOT NULL " +
            "AND ABS(dnl.actualQuantityDelivered - dnl.estimatedQuantity) > :threshold")
    List<DeliveryNoticeLine> findLinesWithSignificantQuantityVariance(@Param("threshold") Integer threshold);
}
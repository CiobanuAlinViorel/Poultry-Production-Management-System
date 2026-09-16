package com.example.slaughterhouse;

import com.example.slaughterhouse.application.service.SlaughterLotMetricsService;
import com.example.slaughterhouse.domain.entities.SlaughterLot;
import com.example.slaughterhouse.domain.valueobjects.Weight;
import com.example.slaughterhouse.infrastructure.persistance.repository.SlaughterLotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class SlaughterLotMetricsServiceTest {

    @Autowired
    private SlaughterLotMetricsService metricsService;

    @Autowired
    private SlaughterLotRepository lotRepository;

    @Test
    @Transactional
    @Rollback(false)
    void testCalculateMetrics() {
        SlaughterLot lot = new SlaughterLot();
        lot.setLotNumber("LOT-200");
        lot.setTotalChickens(20);
        lot.setCurrentQuantity(18); // 2 decedate
        lot.setAverageWeight(Weight.of(2.5f, "kg"));
        lot.setIsActive(true);

        lotRepository.save(lot);

        SlaughterLotMetricsService.SlaughterLotMetrics metrics = metricsService.calculateMetrics(lot.getId());

        assertThat(metrics.totalWeight()).isEqualTo(45f); // 18 * 2.5 = 45
        assertThat(metrics.mortality()).isEqualTo(2);
        assertThat(metrics.mortalityRate()).isEqualTo(10f); // 2/20*100
    }
}

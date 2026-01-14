package com.example.slaughterhouse;

import com.example.slaughterhouse.application.service.DeliveryNoticeService;
import com.example.slaughterhouse.domain.entities.DeliveryNotice;
import com.example.slaughterhouse.domain.entities.SlaughterhouseUser;
import com.example.slaughterhouse.infrastructure.persistance.repository.DeliveryNoticeRepository;
import com.example.slaughterhouse.infrastructure.persistance.repository.SlaughterhouseUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class DeliveryNoticeReceiveTest {

    @Autowired
    private DeliveryNoticeService service;

    @Autowired
    private SlaughterhouseUserRepository userRepository;

    @Autowired
    private DeliveryNoticeRepository deliveryNoticeRepository;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        deliveryNoticeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testReceiveDeliveryNotices() {
        long timestamp = System.currentTimeMillis();

        SlaughterhouseUser employee = new SlaughterhouseUser();
        employee.setUsername("receiver-" + timestamp);
        employee.setFullName("Reception Employee");
        employee.setEmail("receiver" + timestamp + "@example.com");
        employee.setPasswordHash("test");
        employee = userRepository.save(employee);

        DeliveryNotice notice1 = service.createDeliveryNotice(
                "EXT-101-" + timestamp, "Farm A", "LOT-001-" + timestamp, LocalDate.now().plusDays(1),
                50, 2.5f, "Broiler", 30, "Truck A", "AB-001-CD", "John Doe"
        );

        DeliveryNotice notice2 = service.createDeliveryNotice(
                "EXT-102-" + timestamp, "Farm B", "LOT-002-" + timestamp, LocalDate.now().plusDays(1),
                30, 2.4f, "Broiler", 32, "Truck B", "AB-002-CD", "Jane Doe"
        );

        int totalReceived = service.receiveDeliveryNotices(List.of(notice1, notice2), employee);

        assertThat(totalReceived).isEqualTo(80);
        assertThat(notice1.getReceptionStatus().name()).isEqualTo("RECEIVED");
        assertThat(notice2.getReceptionStatus().name()).isEqualTo("RECEIVED");
        assertThat(notice1.getReceivedBy()).isEqualTo(employee);
    }
}
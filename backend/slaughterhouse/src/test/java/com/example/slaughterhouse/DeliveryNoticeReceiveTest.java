package com.example.slaughterhouse;

import com.example.slaughterhouse.application.service.DeliveryNoticeService;
import com.example.slaughterhouse.domain.entities.DeliveryNotice;
import com.example.slaughterhouse.domain.entities.SlaughterhouseUser;
import com.example.slaughterhouse.infrastructure.persistance.repository.SlaughterhouseUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class DeliveryNoticeReceiveTest {

    @Autowired
    private DeliveryNoticeService service;
    @Autowired
    private SlaughterhouseUserRepository userRepository;

    @Test
    @Transactional
    @Rollback(false)
    void testReceiveDeliveryNotices() {
        SlaughterhouseUser employee = new SlaughterhouseUser();
        employee.setUsername("receiver");
        employee.setFullName("Reception Employee");
        employee.setEmail("receiver@example.com");
        employee.setPasswordHash("test");
        userRepository.save(employee);

        DeliveryNotice notice1 = service.createDeliveryNotice(
                "EXT-101", "Farm A", "LOT-001", LocalDate.now().plusDays(1),
                50, 2.5f, "Broiler", 30, "Truck A", "AB-001-CD", "John Doe"
        );

        DeliveryNotice notice2 = service.createDeliveryNotice(
                "EXT-102", "Farm B", "LOT-001", LocalDate.now().plusDays(1),
                30, 2.4f, "Broiler", 32, "Truck B", "AB-002-CD", "Jane Doe"
        );

        int totalReceived = service.receiveDeliveryNotices(List.of(notice1, notice2), employee);

        assertThat(totalReceived).isEqualTo(80);
        assertThat(notice1.getReceptionStatus().name()).isEqualTo("RECEIVED");
        assertThat(notice2.getReceptionStatus().name()).isEqualTo("RECEIVED");
        assertThat(notice1.getReceivedBy()).isEqualTo(employee);
    }
}

package com.example.slaughterhouse;

import com.example.slaughterhouse.application.service.AnteMortemInspectionService;
import com.example.slaughterhouse.domain.entities.AnteMortemInspection;
import com.example.slaughterhouse.domain.entities.SlaughterLot;
import com.example.slaughterhouse.domain.entities.SlaughterhouseUser;
import com.example.slaughterhouse.domain.enums.ApprovalStatus;
import com.example.slaughterhouse.infrastructure.persistance.repository.AnteMortemInspectionRepository;
import com.example.slaughterhouse.infrastructure.persistance.repository.SlaughterLotRepository;
import com.example.slaughterhouse.infrastructure.persistance.repository.SlaughterhouseUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class AnteMortemInspectionServiceTest {

    @Autowired
    private AnteMortemInspectionService inspectionService;

    @Autowired
    private AnteMortemInspectionRepository inspectionRepository;

    @Autowired
    private SlaughterLotRepository lotRepository;

    @Autowired
    private SlaughterhouseUserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        inspectionRepository.deleteAll();
        lotRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testApproveAndRejectInspection() {
        // Creăm un SlaughterLot cu număr unic
        SlaughterLot lot = new SlaughterLot();
        lot.setLotNumber("LOT-003-" + System.currentTimeMillis());
        lot.setSlaughterDate(LocalDate.now().plusDays(1));
        lot.setTotalChickens(50);
        lot.setCurrentQuantity(50);
        lot.setIsActive(true);
        lot = lotRepository.save(lot);

        // Creăm un veterinarian (user) cu email unic
        SlaughterhouseUser vet = new SlaughterhouseUser();
        vet.setUsername("vet1-" + System.currentTimeMillis());
        vet.setFullName("Dr. Veterinarian");
        vet.setEmail("vet" + System.currentTimeMillis() + "@example.com");
        vet.setPasswordHash("test");
        vet = userRepository.save(vet);

        // Creăm inspecția
        AnteMortemInspection inspection = new AnteMortemInspection();
        inspection.setSlaughterLot(lot);
        inspection.setVeterinarian(vet);
        inspection.setInspectionDate(LocalDate.now());
        inspection.setTotalInspected(50);
        inspection.setApproved(0);
        inspection.setRejected(0);
        inspection.setApprovalStatus(ApprovalStatus.PENDING);
        inspection = inspectionRepository.save(inspection);

        // Aprobare
        inspectionService.approveInspection(inspection.getId());
        AnteMortemInspection updatedInspection = inspectionRepository.findById(inspection.getId()).orElseThrow();
        assertThat(updatedInspection.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);

        // Resetăm status pentru test respingere
        updatedInspection.setApprovalStatus(ApprovalStatus.PENDING);
        updatedInspection = inspectionRepository.save(updatedInspection);

        // Respingere
        inspectionService.rejectInspection(inspection.getId(), "Health issues found");
        AnteMortemInspection rejectedInspection = inspectionRepository.findById(inspection.getId()).orElseThrow();
        assertThat(rejectedInspection.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(rejectedInspection.getRejectionReasons()).isEqualTo("Health issues found");
    }
}
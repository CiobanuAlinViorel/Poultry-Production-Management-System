package com.example.slaughterhouse;

import com.example.slaughterhouse.application.service.AnteMortemInspectionService;
import com.example.slaughterhouse.domain.entities.AnteMortemInspection;
import com.example.slaughterhouse.domain.entities.SlaughterLot;
import com.example.slaughterhouse.domain.entities.SlaughterhouseUser;
import com.example.slaughterhouse.domain.enums.ApprovalStatus;
import com.example.slaughterhouse.infrastructure.persistance.repository.AnteMortemInspectionRepository;
import com.example.slaughterhouse.infrastructure.persistance.repository.SlaughterLotRepository;
import com.example.slaughterhouse.infrastructure.persistance.repository.SlaughterhouseUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback(false)
public class AnteMortemInspectionServiceTest {

    @Autowired
    private AnteMortemInspectionService inspectionService;

    @Autowired
    private AnteMortemInspectionRepository inspectionRepository;

    @Autowired
    private SlaughterLotRepository lotRepository;

    @Autowired
    private SlaughterhouseUserRepository userRepository;

    @Test
    @Transactional
    @Rollback(false)
    void testApproveAndRejectInspection() {
        // Creăm un SlaughterLot
        SlaughterLot lot = new SlaughterLot();
        lot.setLotNumber("LOT-003");
        lot.setSlaughterDate(LocalDate.now().plusDays(1));
        lot.setTotalChickens(50);
        lot.setCurrentQuantity(50);
        lot.setIsActive(true);
        lotRepository.save(lot);

        // Creăm un veterinarian (user)
        SlaughterhouseUser vet = new SlaughterhouseUser();
        vet.setUsername("vet1");
        vet.setFullName("Dr. Veterinarian");
        vet.setEmail("vet1@example.com");
        vet.setPasswordHash("test");
        userRepository.save(vet);

        // Creăm inspecția
        AnteMortemInspection inspection = new AnteMortemInspection();
        inspection.setSlaughterLot(lot);
        inspection.setVeterinarian(vet);
        inspection.setInspectionDate(LocalDate.now());
        inspection.setTotalInspected(50);
        inspection.setApproved(0);
        inspection.setRejected(0);
        inspection.setApprovalStatus(ApprovalStatus.PENDING);
        inspectionRepository.save(inspection);

        // Aprobare
        inspectionService.approveInspection(inspection.getId());
        AnteMortemInspection updatedInspection = inspectionRepository.findById(inspection.getId()).orElseThrow();
        assertThat(updatedInspection.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);

        // Resetăm status pentru test respingere
        updatedInspection.setApprovalStatus(ApprovalStatus.PENDING);
        inspectionRepository.save(updatedInspection);

        // Respingere
        inspectionService.rejectInspection(inspection.getId(), "Health issues found");
        AnteMortemInspection rejectedInspection = inspectionRepository.findById(inspection.getId()).orElseThrow();
        assertThat(rejectedInspection.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(rejectedInspection.getRejectionReasons()).isEqualTo("Health issues found");
    }
}

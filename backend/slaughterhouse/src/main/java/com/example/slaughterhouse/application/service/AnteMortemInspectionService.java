package com.example.slaughterhouse.application.service;

import com.example.slaughterhouse.domain.entities.AnteMortemInspection;
import com.example.slaughterhouse.domain.enums.ApprovalStatus;
import com.example.slaughterhouse.infrastructure.persistance.repository.AnteMortemInspectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnteMortemInspectionService {

    @Autowired
    private AnteMortemInspectionRepository inspectionRepository;

    @Transactional
    public void approveInspection(Long inspectionId) {
        AnteMortemInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found"));

        inspection.approve();
        inspectionRepository.save(inspection);
    }

    @Transactional
    public void rejectInspection(Long inspectionId, String reasons) {
        AnteMortemInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found"));

        inspection.reject(reasons);
        inspectionRepository.save(inspection);
    }
}

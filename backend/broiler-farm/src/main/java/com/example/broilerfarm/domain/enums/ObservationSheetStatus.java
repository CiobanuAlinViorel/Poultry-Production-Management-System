package com.example.broilerfarm.domain.enums;

public enum ObservationSheetStatus {
    DRAFT,      // Farm Manager completează
    SUBMITTED,  // Trimis pentru validare
    APPROVED,   // Aprobat - trigger delivery eligibility check
    REJECTED    // Respins, necesită corecții
}
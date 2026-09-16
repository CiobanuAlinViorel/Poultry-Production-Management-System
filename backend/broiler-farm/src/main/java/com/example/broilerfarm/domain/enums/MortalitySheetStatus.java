package com.example.broilerfarm.domain.enums;

public enum MortalitySheetStatus {
    DRAFT,      // Worker editează
    SUBMITTED,  // Trimis la aprobare
    APPROVED,   // Aprobat de Farm Manager
    REJECTED    // Respins, trebuie corectat
}
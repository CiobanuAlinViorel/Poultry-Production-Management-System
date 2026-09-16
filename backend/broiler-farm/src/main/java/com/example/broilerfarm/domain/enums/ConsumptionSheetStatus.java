package com.example.broilerfarm.domain.enums;

public enum ConsumptionSheetStatus {
    DRAFT,      // Logistics Manager editează
    SUBMITTED,  // Trimis la validare
    APPROVED,   // Aprobat, stocuri actualizate
    REJECTED    // Respins, trebuie corectat
}
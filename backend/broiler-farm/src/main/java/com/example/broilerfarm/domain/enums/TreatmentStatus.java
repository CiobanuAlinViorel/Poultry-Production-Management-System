package com.example.broilerfarm.domain.enums;

public enum TreatmentStatus {
    DRAFT,       // În creare, veterinar editează
    ACTIVE,      // Aprobat și în derulare
    COMPLETED    // Finalizat (dar poate fi în withdrawal)
}
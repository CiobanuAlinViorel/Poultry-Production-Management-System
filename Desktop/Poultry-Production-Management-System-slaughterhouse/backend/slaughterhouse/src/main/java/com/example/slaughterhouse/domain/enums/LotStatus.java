package com.example.slaughterhouse.domain.enums;

/**
 * Status of a slaughter lot throughout the processing pipeline
 */
public enum LotStatus {
    RECEIVED("Received from farm"),
    AWAITING_INSPECTION("Awaiting ante-mortem inspection"),
    INSPECTION_APPROVED("Inspection approved, ready for slaughter"),
    INSPECTION_REJECTED("Inspection rejected"),
    IN_PROCESSING("Currently being processed"),
    PROCESSING_COMPLETE("Processing completed"),
    PACKAGED("Products packaged"),
    IN_STORAGE("In cold storage"),
    READY_FOR_DELIVERY("Ready for delivery to clients"),
    DELIVERED("Delivered to clients"),
    PENDING("PENDING"),
    CLOSED("Lot closed");

    private final String description;

    LotStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

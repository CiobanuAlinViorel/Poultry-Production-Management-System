package com.example.slaughterhouse.domain.enums;

/**
 * Status of client delivery notice
 */
public enum ClientDeliveryStatus {
    DRAFT("Draft"),
    PENDING_APPROVAL("Pending approval"),
    APPROVED("Approved"),
    SCHEDULED("Scheduled"),
    IN_TRANSIT("In transit"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled"),
    FAILED("Delivery failed");

    private final String description;

    ClientDeliveryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
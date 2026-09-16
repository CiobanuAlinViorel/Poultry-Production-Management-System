package com.example.slaughterhouse.domain.enums;

/**
 * Status of transport
 */
public enum TransportStatus {
    SCHEDULED("Scheduled"),
    LOADING("Loading in progress"),
    IN_TRANSIT("In transit"),
    DELAYED("Delayed"),
    ARRIVED("Arrived at destination"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled"),
    INCIDENT("Incident reported");

    private final String description;

    TransportStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
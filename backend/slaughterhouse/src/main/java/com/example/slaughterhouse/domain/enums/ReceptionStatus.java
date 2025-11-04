package com.example.slaughterhouse.domain.enums;

/**
 * Status of delivery notice reception
 */
public enum ReceptionStatus {
    PENDING("Pending reception"),
    RECEIVED("Received"),
    CONFIRMED("Confirmed"),
    PROCESSING("Processing"),
    COMPLETED("Reception completed"),
    REJECTED("Reception rejected");

    private final String description;

    ReceptionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
package com.example.slaughterhouse.domain.enums;

/**
 * Status of items in cold storage
 */
public enum StorageStatus {
    STORED("Currently stored"),
    REMOVED("Removed from storage"),
    RELOCATED("Relocated to another location"),
    EXPIRED("Expired while in storage"),
    DAMAGED("Damaged in storage");

    private final String description;

    StorageStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
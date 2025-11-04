package com.example.slaughterhouse.domain.enums;

/**
 * Status of a package
 */
public enum PackageStatus {
    PACKAGED("Packaged"),
    IN_STORAGE("In cold storage"),
    READY_FOR_DELIVERY("Ready for delivery"),
    LOADING("Being loaded"),
    IN_TRANSIT("In transit"),
    DELIVERED("Delivered"),
    DAMAGED("Damaged"),
    EXPIRED("Expired");

    private final String description;

    PackageStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
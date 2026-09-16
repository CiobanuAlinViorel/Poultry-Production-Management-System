package com.example.slaughterhouse.domain.enums;

/**
 * Status of a product
 */
public enum ProductStatus {
    PRODUCED("Produced"),
    INSPECTED("Inspected"),
    APPROVED("Approved for packaging"),
    REJECTED("Rejected"),
    PACKAGED("Packaged"),
    IN_STORAGE("In cold storage"),
    READY_FOR_DELIVERY("Ready for delivery"),
    DELIVERED("Delivered");

    private final String description;

    ProductStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
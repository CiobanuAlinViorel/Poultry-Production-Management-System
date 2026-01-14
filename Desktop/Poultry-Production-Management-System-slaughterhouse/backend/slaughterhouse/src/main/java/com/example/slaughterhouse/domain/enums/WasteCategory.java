package com.example.slaughterhouse.domain.enums;

/**
 * Categories of waste
 */
public enum WasteCategory {
    ORGANIC("Organic waste - feathers, blood, viscera"),
    REJECTED_CARCASS("Rejected carcasses"),
    PROCESSING_WASTE("Processing waste"),
    PACKAGING_WASTE("Packaging materials"),
    GENERAL("General waste"),
    HAZARDOUS("Hazardous waste");

    private final String description;

    WasteCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
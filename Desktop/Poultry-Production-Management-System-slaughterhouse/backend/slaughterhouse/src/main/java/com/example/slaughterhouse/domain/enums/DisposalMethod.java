package com.example.slaughterhouse.domain.enums;

/**
 * Methods for waste disposal
 */
public enum DisposalMethod {
    INCINERATION("Incineration"),
    BURIAL("Burial in designated area"),
    COMPOSTING("Composting"),
    RENDERING("Rendering plant"),
    BIOGAS("Biogas production"),
    LANDFILL("Sanitary landfill"),
    RECYCLING("Recycling"),
    THIRD_PARTY("Third-party waste management");

    private final String description;

    DisposalMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
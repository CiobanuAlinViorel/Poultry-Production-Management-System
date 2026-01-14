package com.example.slaughterhouse.domain.enums;

/**
 * Types of products from slaughter processing
 */
public enum ProductType {
    WHOLE_CHICKEN("Whole Chicken"),
    BREAST("Chicken Breast"),
    THIGH("Chicken Thigh"),
    DRUMSTICK("Chicken Drumstick"),
    WING("Chicken Wing"),
    LIVER("Chicken Liver"),
    HEART("Chicken Heart"),
    GIZZARD("Chicken Gizzard"),
    FEET("Chicken Feet"),
    MINCED("Minced Chicken"),
    OTHER("Other Product");

    private final String displayName;

    ProductType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
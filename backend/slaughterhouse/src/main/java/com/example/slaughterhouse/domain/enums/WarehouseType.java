package com.example.slaughterhouse.domain.enums;

/**
 * Types of warehouses
 */
public enum WarehouseType {
    COLD_STORAGE("Cold Storage"),
    FREEZER("Freezer Storage"),
    REFRIGERATED("Refrigerated Storage"),
    DRY_STORAGE("Dry Storage"),
    QUARANTINE("Quarantine Storage");

    private final String displayName;

    WarehouseType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
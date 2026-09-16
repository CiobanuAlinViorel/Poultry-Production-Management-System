package com.example.slaughterhouse.domain.enums;

/**
 * Types of packaging
 */
public enum PackageType {
    VACUUM_SEALED("Vacuum Sealed"),
    TRAY_PACK("Tray Pack"),
    BULK("Bulk Package"),
    RETAIL_BOX("Retail Box"),
    INDUSTRIAL("Industrial Package"),
    FROZEN_BAG("Frozen Bag");

    private final String displayName;

    PackageType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
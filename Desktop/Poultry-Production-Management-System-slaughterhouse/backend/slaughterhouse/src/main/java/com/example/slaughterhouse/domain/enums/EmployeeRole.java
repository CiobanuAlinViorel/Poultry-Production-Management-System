package com.example.slaughterhouse.domain.enums;

/**
 * Roles for employees in the slaughterhouse system
 */
public enum EmployeeRole {
    SLAUGHTERHOUSE_MANAGER("Slaughterhouse Manager"),
    VETERINARIAN("Veterinarian"),
    QUALITY_CONTROL("Quality Control Officer"),
    PROCESSING_WORKER("Processing Worker"),
    PACKAGING_WORKER("Packaging Worker"),
    WAREHOUSE_MANAGER("Warehouse Manager"),
    COLD_STORAGE_MANAGER("Cold Storage Manager"),
    TRANSPORT_COORDINATOR("Transport Coordinator"),
    RECEPTION_CLERK("Reception Clerk"),
    ADMIN("Administrator");

    private final String displayName;

    EmployeeRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
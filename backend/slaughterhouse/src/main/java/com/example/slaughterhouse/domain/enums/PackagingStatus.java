
package com.example.slaughterhouse.domain.enums;

/**
 * Status of packaging sheet
 */
public enum PackagingStatus {
    DRAFT("Draft"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed"),
    VERIFIED("Verified"),
    CANCELLED("Cancelled");

    private final String description;

    PackagingStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
package com.example.slaughterhouse.domain.enums;

/**
 * Approval status for inspections
 */
public enum ApprovalStatus {
    PENDING("Pending approval"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CONDITIONAL("Conditionally approved"),
    UNDER_REVIEW("Under review");

    private final String description;

    ApprovalStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
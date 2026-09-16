package com.example.slaughterhouse.domain.valueobjects;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Value Object representing health status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class HealthStatus implements Serializable {

    private String status; // "HEALTHY", "SICK", "QUARANTINE", "CRITICAL"
    private String description;
    private String severity; // "LOW", "MEDIUM", "HIGH", "CRITICAL"

    public static HealthStatus healthy() {
        return new HealthStatus("HEALTHY", "No health issues detected", "LOW");
    }

    public static HealthStatus sick(String description) {
        return new HealthStatus("SICK", description, "MEDIUM");
    }

    public static HealthStatus critical(String description) {
        return new HealthStatus("CRITICAL", description, "CRITICAL");
    }

    public Boolean isHealthy() {
        return "HEALTHY".equalsIgnoreCase(status);
    }

    public Boolean requiresQuarantine() {
        return "QUARANTINE".equalsIgnoreCase(status) ||
                "CRITICAL".equalsIgnoreCase(severity);
    }

    public Boolean isAcceptableForSlaughter() {
        return "HEALTHY".equalsIgnoreCase(status) ||
                ("SICK".equalsIgnoreCase(status) && "LOW".equalsIgnoreCase(severity));
    }
}
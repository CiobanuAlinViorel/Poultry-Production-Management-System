package com.example.slaughterhouse.domain.valueobjects;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Value Object representing weight with value and unit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Weight implements Serializable {

    private Float value;
    private String unit; // "KG", "G", "LBS"

    public static Weight of(Float value, String unit) {
        return new Weight(value, unit);
    }

    public Weight convertToKilograms() {
        if ("KG".equalsIgnoreCase(unit)) {
            return this;
        } else if ("G".equalsIgnoreCase(unit)) {
            return new Weight(value / 1000, "KG");
        } else if ("LBS".equalsIgnoreCase(unit)) {
            return new Weight(value * 0.453592f, "KG");
        }
        throw new IllegalArgumentException("Unknown unit: " + unit);
    }
}
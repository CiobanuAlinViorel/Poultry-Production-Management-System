package com.example.slaughterhouse.domain.valueobjects;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Value Object representing temperature with value and unit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Temperature implements Serializable {

    private Float value;
    private String unit; // "C", "F", "K"

    public static Temperature celsius(Float value) {
        return new Temperature(value, "C");
    }

    public static Temperature fahrenheit(Float value) {
        return new Temperature(value, "F");
    }

    public Temperature convertToCelsius() {
        if ("C".equalsIgnoreCase(unit)) {
            return this;
        } else if ("F".equalsIgnoreCase(unit)) {
            return new Temperature((value - 32) * 5/9, "C");
        } else if ("K".equalsIgnoreCase(unit)) {
            return new Temperature(value - 273.15f, "C");
        }
        throw new IllegalArgumentException("Unknown unit: " + unit);
    }

    public Boolean isSafe() {
        Temperature celsius = convertToCelsius();
        // Cold storage should be between -2°C and 4°C
        return celsius.value >= -2.0f && celsius.value <= 4.0f;
    }

    public Boolean isCritical() {
        Temperature celsius = convertToCelsius();
        return celsius.value > 10.0f || celsius.value < -5.0f;
    }
}
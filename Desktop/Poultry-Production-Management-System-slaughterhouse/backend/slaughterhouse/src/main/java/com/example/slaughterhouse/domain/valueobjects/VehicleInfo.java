package com.example.slaughterhouse.domain.valueobjects;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Value Object representing vehicle information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class VehicleInfo implements Serializable {

    private String vehicleType; // "REFRIGERATED_TRUCK", "VAN", "TRAILER"
    private String vehiclePlate;
    private Integer capacity; // in packages or cubic meters

    public static VehicleInfo of(String vehicleType, String vehiclePlate, Integer capacity) {
        return new VehicleInfo(vehicleType, vehiclePlate, capacity);
    }

    public String getInfo() {
        return String.format("%s [%s] - Capacity: %d", vehicleType, vehiclePlate, capacity);
    }

    public Boolean hasCapacityFor(Integer requiredPackages) {
        return capacity != null && capacity >= requiredPackages;
    }

    public Boolean isRefrigerated() {
        return vehicleType != null && vehicleType.contains("REFRIGERATED");
    }

    public Boolean isValid() {
        return vehicleType != null && !vehicleType.isEmpty() &&
                vehiclePlate != null && !vehiclePlate.isEmpty() &&
                capacity != null && capacity > 0;
    }
}
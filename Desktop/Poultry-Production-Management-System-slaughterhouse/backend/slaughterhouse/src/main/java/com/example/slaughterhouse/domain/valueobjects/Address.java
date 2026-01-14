package com.example.slaughterhouse.domain.valueobjects;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Value Object representing an address
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Address implements Serializable {

    private String street;
    private String city;
    private String postalCode;
    private String country;

    public static Address of(String street, String city, String postalCode, String country) {
        return new Address(street, city, postalCode, country);
    }

    public String getFullAddress() {
        return String.format("%s, %s, %s, %s", street, city, postalCode, country);
    }

    public Boolean isComplete() {
        return street != null && !street.isEmpty() &&
                city != null && !city.isEmpty() &&
                postalCode != null && !postalCode.isEmpty() &&
                country != null && !country.isEmpty();
    }
}
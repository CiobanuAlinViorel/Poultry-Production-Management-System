package com.example.slaughterhouse.domain.valueobjects;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Value Object representing a package code
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PackageCode implements Serializable {

    private String prefix; // e.g., "PKG", "SH"
    private Integer number; // Sequential number
    private String suffix; // e.g., date code, batch code

    public static PackageCode generate(String prefix, Integer number) {
        String dateSuffix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return new PackageCode(prefix, number, dateSuffix);
    }

    public static PackageCode of(String prefix, Integer number, String suffix) {
        return new PackageCode(prefix, number, suffix);
    }

    public String getFullCode() {
        return String.format("%s-%06d-%s", prefix, number, suffix);
    }

    public Boolean isValid() {
        return prefix != null && !prefix.isEmpty() &&
                number != null && number > 0 &&
                suffix != null && !suffix.isEmpty();
    }
}
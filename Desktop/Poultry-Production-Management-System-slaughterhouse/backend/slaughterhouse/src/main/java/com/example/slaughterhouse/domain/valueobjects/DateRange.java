package com.example.slaughterhouse.domain.valueobjects;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Value Object representing a date range
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class DateRange implements Serializable {

    private LocalDate startDate;
    private LocalDate endDate;

    public static DateRange of(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        return new DateRange(startDate, endDate);
    }

    public Long getDurationInDays() {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    public Boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public Boolean overlaps(DateRange other) {
        return !this.endDate.isBefore(other.startDate) &&
                !other.endDate.isBefore(this.startDate);
    }

    public Boolean isValid() {
        return startDate != null && endDate != null && !startDate.isAfter(endDate);
    }
}
package com.example.slaughterhouse.domain.valueobjects;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Value Object representing quality grade
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class QualityGrade implements Serializable {

    private String grade; // "A", "B", "C", "D"
    private Integer score; // 0-100
    private String description;

    public static QualityGrade gradeA(String description) {
        return new QualityGrade("A", 90, description);
    }

    public static QualityGrade gradeB(String description) {
        return new QualityGrade("B", 75, description);
    }

    public static QualityGrade gradeC(String description) {
        return new QualityGrade("C", 60, description);
    }

    public static QualityGrade fromScore(Integer score, String description) {
        if (score >= 90) {
            return new QualityGrade("A", score, description);
        } else if (score >= 75) {
            return new QualityGrade("B", score, description);
        } else if (score >= 60) {
            return new QualityGrade("C", score, description);
        } else {
            return new QualityGrade("D", score, description);
        }
    }

    public Boolean isAcceptable() {
        return score != null && score >= 60;
    }

    public Boolean isPremium() {
        return "A".equalsIgnoreCase(grade) && score >= 90;
    }
}
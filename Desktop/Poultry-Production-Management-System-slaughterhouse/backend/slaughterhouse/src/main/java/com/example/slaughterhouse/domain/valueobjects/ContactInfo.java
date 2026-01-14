package com.example.slaughterhouse.domain.valueobjects;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Value Object representing contact information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ContactInfo implements Serializable {

    private String email;
    private String phone;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[+]?[0-9]{10,15}$");

    public static ContactInfo of(String email, String phone) {
        ContactInfo contactInfo = new ContactInfo(email, phone);
        if (!contactInfo.isValid()) {
            throw new IllegalArgumentException("Invalid contact information");
        }
        return contactInfo;
    }

    public Boolean isValid() {
        return isEmailValid() && isPhoneValid();
    }

    public Boolean isEmailValid() {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public Boolean isPhoneValid() {
        if (phone == null) return false;
        String cleanPhone = phone.replaceAll("[\\s()-]", "");
        return PHONE_PATTERN.matcher(cleanPhone).matches();
    }

    public String getFormattedPhone() {
        if (phone == null) return "";
        String cleaned = phone.replaceAll("[^0-9+]", "");
        if (cleaned.startsWith("+")) {
            return cleaned;
        }
        return cleaned;
    }
}
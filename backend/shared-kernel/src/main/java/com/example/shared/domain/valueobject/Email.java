package com.example.shared.domain.valueobject;

import com.example.shared.domain.exception.InvalidEmailException;
import lombok.Value;

import java.util.regex.Pattern;

@Value
public class Email {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    String address;

    public Email(String address) {
        if (address == null || address.isBlank()) {
            throw new InvalidEmailException("Email cannot be null or empty");
        }
        if (!EMAIL_PATTERN.matcher(address).matches()) {
            throw new InvalidEmailException("Invalid email format: " + address);
        }
        this.address = address.toLowerCase();
    }

    public String getLocalPart() {
        return address.substring(0, address.indexOf('@'));
    }

    public String getDomain() {
        return address.substring(address.indexOf('@') + 1);
    }

    public boolean isValid() {
        return EMAIL_PATTERN.matcher(address).matches();
    }

    @Override
    public String toString() {
        return address;
    }
}
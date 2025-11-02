package com.example.shared.security;

import com.example.shared.domain.User;

public class AuthenticationResult {
    private final boolean success;
    private final User user;
    private final String token;
    private final String refreshToken;
    private final boolean passwordChangeRequired;
    private final String message;

    private AuthenticationResult(boolean success, User user, String token,
                                 String refreshToken, boolean passwordChangeRequired, String message) {
        this.success = success;
        this.user = user;
        this.token = token;
        this.refreshToken = refreshToken;
        this.passwordChangeRequired = passwordChangeRequired;
        this.message = message;
    }

    // Factory methods
    public static AuthenticationResult success(User user, String token, String refreshToken) {
        return new AuthenticationResult(true, user, token, refreshToken, false, "Login successful");
    }

    public static AuthenticationResult passwordChangeRequired(User user) {
        return new AuthenticationResult(false, user, null, null, true, "Password change required");
    }

    public static AuthenticationResult failure(String message) {
        return new AuthenticationResult(false, null, null, null, false, message);
    }

    // Getters
    public boolean isSuccess() { return success; }
    public User getUser() { return user; }
    public String getToken() { return token; }
    public String getRefreshToken() { return refreshToken; }
    public boolean isPasswordChangeRequired() { return passwordChangeRequired; }
    public String getMessage() { return message; }
}
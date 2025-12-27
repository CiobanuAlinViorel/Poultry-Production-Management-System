package com.example.broilerfarm.application.dto.create_employee_user;

@lombok.Data
@lombok.AllArgsConstructor
public class PasswordResetResponse {
    private String message;
    private String newPassword;
}

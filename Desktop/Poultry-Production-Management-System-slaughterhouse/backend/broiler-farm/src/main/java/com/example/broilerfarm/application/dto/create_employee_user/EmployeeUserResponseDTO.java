package com.example.broilerfarm.application.dto.create_employee_user;



@lombok.Data
public class EmployeeUserResponseDTO {
    private Long id;
    private String username;
    private String email;
    private Boolean isActive;
    private Boolean accountLocked;
    private java.util.Set<String> roles;
    private java.time.LocalDateTime lastLoginDate;
    private Boolean mustChangePassword;
    private EmployeeDTO employee;
    private String generatedPassword;  // Doar la creare
}

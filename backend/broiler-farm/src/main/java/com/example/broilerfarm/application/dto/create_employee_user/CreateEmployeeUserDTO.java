package com.example.broilerfarm.application.dto.create_employee_user;

import com.example.shared.domain.enums.Role;

import java.time.LocalDate;

@lombok.Data
public class CreateEmployeeUserDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String username;
    private Role role;
    private LocalDate hireDate;
    private Long farmId;
}
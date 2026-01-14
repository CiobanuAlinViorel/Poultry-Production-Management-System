package com.example.broilerfarm.application.dto.create_employee_user;

import com.example.shared.domain.enums.Role;

import java.time.LocalDate;

@lombok.Data
public class EmployeeDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Role role;
    private LocalDate hireDate;
}
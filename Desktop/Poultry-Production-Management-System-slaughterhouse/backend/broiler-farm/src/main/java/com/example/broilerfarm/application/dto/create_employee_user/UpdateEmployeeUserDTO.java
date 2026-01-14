package com.example.broilerfarm.application.dto.create_employee_user;

import com.example.shared.domain.enums.Role;

@lombok.Data
public class UpdateEmployeeUserDTO {
    private String firstName;
    private String lastName;
    private String phone;
    private String username;
    private Role role;
    private Long farmId;
}
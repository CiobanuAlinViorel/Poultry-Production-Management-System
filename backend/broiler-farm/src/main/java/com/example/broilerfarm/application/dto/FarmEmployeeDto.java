package com.example.broilerfarm.application.dto;
import com.example.shared.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FarmEmployeeDto {
    private Long  id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Role role;
    private LocalDate hireDate;
    private Long broilerFarmId;
}

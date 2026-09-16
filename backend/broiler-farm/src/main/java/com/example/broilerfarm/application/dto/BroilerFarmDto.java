package com.example.broilerfarm.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class BroilerFarmDto {
    private Long id;
    private String farmName;
    private String location;
    private String address;
    private Integer capacity;
    private String licenseNumber;
}

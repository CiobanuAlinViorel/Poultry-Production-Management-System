package com.example.broilerfarm.application.dto;

import com.example.broilerfarm.domain.enums.QualityGrade;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChicksReceptionLineDto {
     private Long id;
     private Long receptionId;
     private Long poultryHouseId;
     private String lotNumber ;
     private Integer quantity;
     private  Integer chicksAlive;
     private Integer chicksDOA;
     private Integer chicksWeak;
     private QualityGrade qualityGrade;
     private String notes;
     private String breed;
     private String hatcherySource;
}

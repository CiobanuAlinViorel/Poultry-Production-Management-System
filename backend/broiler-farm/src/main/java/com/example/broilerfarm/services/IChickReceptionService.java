package com.example.broilerfarm.services;

import com.example.broilerfarm.domain.enums.QualityGrade;

public interface IChickReceptionService {
    // (1) Add line to reception
    public void addLine(Long receptionId, Long poultryHouseId, Integer chicksAlive, Integer chicksDOA, Integer chicksWeak, QualityGrade qualityGrade, String notes);

    // (2) Update line from reception
    public void updateLine(Long id, Integer chicksAlive, Integer chicksDOA, Integer chicksWeak);

    // (3) Delete line from reception
    public void deleteLine(Long id);
}

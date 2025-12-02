package com.example.broilerfarm.services;

import com.example.broilerfarm.domain.entities.ChicksReception;
import com.example.broilerfarm.domain.entities.ChicksReceptionLine;
import com.example.broilerfarm.domain.entities.PoultryHouse;
import com.example.broilerfarm.domain.enums.QualityGrade;
import com.example.broilerfarm.infrastructure.persistence.repositories.ChicksReceptionLineRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.ChicksReceptionRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.PoultryHouseRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ChicksReceptionServiceImplementation implements IChickReceptionService{
    @Autowired
    private ChicksReceptionRepository chicksReceptionRepository;

    @Autowired
    private PoultryHouseRepository poultryHouseRepository;

    @Autowired
    private ChicksReceptionLineRepository chicksReceptionLineRepository;


    @Override
    public void addLine(Long receptionId, Long poultryHouseId, Integer chicksAlive, Integer chicksDOA, Integer chicksWeak, QualityGrade qualityGrade, String notes) {
        if(chicksAlive < 0 || chicksDOA < 0 || chicksWeak < 0 || qualityGrade == null) {
            throw new IllegalArgumentException("The values must be positive or 0");
        }

        ChicksReception chicksReception = chicksReceptionRepository.findById(receptionId).orElse(null);
        if(chicksReception == null){
            throw new RuntimeException("ChicksReception could not be found");
        }

        // ✅ CORECTAT: folosește findById în loc de findByPoultryHouseId
        PoultryHouse poultryHouse = poultryHouseRepository.findById(poultryHouseId).orElse(null);
        if(poultryHouse == null){
            throw new RuntimeException("PoultryHouse could not be found");
        }

        ChicksReceptionLine chicksReceptionLine = new ChicksReceptionLine(
                chicksReception,
                poultryHouse,
                null,
                (chicksAlive + chicksDOA + chicksWeak),
                chicksAlive,
                chicksDOA,
                chicksWeak,
                qualityGrade,
                notes
        );

        chicksReception.addReceptionLine(chicksReceptionLine);
        chicksReceptionRepository.save(chicksReception);
    }

    @Override
    public void updateLine(Long id, Integer chicksAlive, Integer chicksDOA, Integer chicksWeak) {
        if(chicksAlive < 0 || chicksDOA < 0 || chicksWeak < 0){
            throw new RuntimeException("The quantities must be positive");
        }

        if(id == null){
            throw new EntityNotFoundException("The id must be indicated: "+ id);
        }

        ChicksReceptionLine line = chicksReceptionLineRepository.findById(id).orElse(null);
        if(line == null){
            throw new RuntimeException("ChicksReceptionLine could not be found");
        }

        line.setChicksAlive(chicksAlive);
        line.setChicksDOA(chicksDOA);
        line.setChicksWeak(chicksWeak);
        line.setQuantity(chicksAlive + chicksDOA + chicksWeak);

        line.getReception().recalculateTotals();
        chicksReceptionLineRepository.save(line);
    }

    @Override
    public void deleteLine(Long id) {
        if(id == null){
            throw new EntityNotFoundException("id could not be : " + id);
        }

        ChicksReceptionLine line = chicksReceptionLineRepository.findById(id).orElse(null);
        if(line == null){
            throw new RuntimeException("ChicksReceptionLine could not be found");
        }

        ChicksReception reception = line.getReception();
        reception.removeReceptionLine(line);
        chicksReceptionRepository.save(reception);
        chicksReceptionLineRepository.delete(line);
    }
}
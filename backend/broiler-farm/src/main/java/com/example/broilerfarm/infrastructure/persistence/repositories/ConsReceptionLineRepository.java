package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.ConsReceptionLine;
import com.example.broilerfarm.domain.enums.QualityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsReceptionLineRepository extends JpaRepository<ConsReceptionLine, Long> {

    
}
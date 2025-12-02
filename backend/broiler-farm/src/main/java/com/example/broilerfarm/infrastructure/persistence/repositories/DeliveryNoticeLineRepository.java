package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.DeliveryNoticeLine;
import com.example.broilerfarm.domain.enums.QualityGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryNoticeLineRepository extends JpaRepository<DeliveryNoticeLine, Long> {

    
}
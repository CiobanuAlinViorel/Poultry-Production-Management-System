package com.ppms.reporting.repositories;

import com.ppms.reporting.domain.LotFact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LotFactRepository extends JpaRepository<LotFact, UUID> {
}

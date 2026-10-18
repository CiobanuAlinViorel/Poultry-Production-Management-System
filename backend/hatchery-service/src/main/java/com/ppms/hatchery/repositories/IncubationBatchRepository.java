package com.ppms.hatchery.repositories;

import com.ppms.hatchery.domain.IncubationBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncubationBatchRepository extends JpaRepository<IncubationBatch, UUID> {

    List<IncubationBatch> findByHatcheryId(UUID hatcheryId);
}

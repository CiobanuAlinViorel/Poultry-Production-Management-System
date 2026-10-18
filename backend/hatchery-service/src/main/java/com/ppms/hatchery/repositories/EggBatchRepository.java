package com.ppms.hatchery.repositories;

import com.ppms.hatchery.domain.EggBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EggBatchRepository extends JpaRepository<EggBatch, UUID> {

    List<EggBatch> findByHatcheryId(UUID hatcheryId);
}

package com.ppms.hatchery.repositories;

import com.ppms.hatchery.domain.ChickBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChickBatchRepository extends JpaRepository<ChickBatch, UUID> {

    List<ChickBatch> findByHatcheryId(UUID hatcheryId);
}

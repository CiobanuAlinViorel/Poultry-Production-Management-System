package com.ppms.hatchery.repositories;

import com.ppms.hatchery.domain.Incubator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IncubatorRepository extends JpaRepository<Incubator, UUID> {

    List<Incubator> findByHatcheryId(UUID hatcheryId);
}

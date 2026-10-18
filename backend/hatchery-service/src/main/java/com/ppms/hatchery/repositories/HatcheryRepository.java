package com.ppms.hatchery.repositories;

import com.ppms.hatchery.domain.Hatchery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface HatcheryRepository extends JpaRepository<Hatchery, UUID> {
}

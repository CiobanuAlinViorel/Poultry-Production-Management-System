package com.ppms.parentflock.repository;

import com.ppms.parentflock.domain.Flock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FlockRepository extends JpaRepository<Flock, UUID> {

    List<Flock> findBySiteId(UUID siteId);

    List<Flock> findBySiteIdAndHouseId(UUID siteId, UUID houseId);

    boolean existsByHouseIdAndStatus(UUID houseId, Flock.FlockStatus status);
}

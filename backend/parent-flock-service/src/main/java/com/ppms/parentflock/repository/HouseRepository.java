package com.ppms.parentflock.repository;

import com.ppms.parentflock.domain.House;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HouseRepository extends JpaRepository<House, UUID> {

    List<House> findBySiteId(UUID siteId);
}

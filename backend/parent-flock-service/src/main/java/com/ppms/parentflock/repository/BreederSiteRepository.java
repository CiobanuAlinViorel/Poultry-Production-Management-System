package com.ppms.parentflock.repository;

import com.ppms.parentflock.domain.BreederSite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BreederSiteRepository extends JpaRepository<BreederSite, UUID> {
}

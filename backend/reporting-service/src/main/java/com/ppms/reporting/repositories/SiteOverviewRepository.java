package com.ppms.reporting.repositories;

import com.ppms.reporting.domain.SiteOverview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SiteOverviewRepository extends JpaRepository<SiteOverview, UUID> {
}

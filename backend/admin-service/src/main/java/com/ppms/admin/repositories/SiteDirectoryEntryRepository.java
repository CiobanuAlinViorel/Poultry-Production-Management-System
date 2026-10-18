package com.ppms.admin.repositories;

import com.ppms.admin.domain.SiteDirectoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SiteDirectoryEntryRepository extends JpaRepository<SiteDirectoryEntry, UUID> {

    Optional<SiteDirectoryEntry> findBySiteId(UUID siteId);

    List<SiteDirectoryEntry> findBySiteIdIn(Collection<UUID> siteIds);

    List<SiteDirectoryEntry> findBySiteTypeOrderByName(String siteType);
}

package com.ppms.admin.repositories;

import com.ppms.admin.domain.SiteDirectoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SiteDirectoryEntryRepository extends JpaRepository<SiteDirectoryEntry, UUID> {
}

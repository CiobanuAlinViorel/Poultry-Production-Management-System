package com.ppms.slaughterhouse.repositories;

import com.ppms.slaughterhouse.domain.SlaughterSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SlaughterSessionRepository extends JpaRepository<SlaughterSession, UUID> {

    List<SlaughterSession> findBySlaughterhouseId(UUID slaughterhouseId);
    boolean existsBySessionCodeIgnoreCase(String sessionCode);
}

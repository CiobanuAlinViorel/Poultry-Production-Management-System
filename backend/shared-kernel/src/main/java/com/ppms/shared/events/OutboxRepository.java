package com.ppms.shared.events;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEntity, UUID> {

    @Query(value = "SELECT * FROM outbox_event WHERE published_at IS NULL ORDER BY created_at LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEntity> findPending(@Param("limit") int limit);

    @Modifying
    @Query("UPDATE OutboxEntity o SET o.publishedAt = :now, o.attempts = o.attempts + 1 WHERE o.id = :id")
    void markPublished(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE OutboxEntity o SET o.attempts = o.attempts + 1, o.lastError = :error WHERE o.id = :id")
    void markError(@Param("id") UUID id, @Param("error") String error);

    @Modifying
    @Query("DELETE FROM OutboxEntity o WHERE o.publishedAt IS NOT NULL AND o.publishedAt < :cutoff")
    int deletePublishedOlderThan(@Param("cutoff") Instant cutoff);
}

package com.ppms.admin.repositories.settings;

import com.ppms.admin.domain.settings.SettingValue;
import com.ppms.shared.domain.ScopeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettingValueRepository extends JpaRepository<SettingValue, UUID> {

    @Query("""
            select v from SettingValue v
            where v.definitionKey = :key
              and v.scopeType = :scopeType
              and ((:scopeId) is null and v.scopeId is null or (:scopeId) is not null and v.scopeId = :scopeId)
              and v.effectiveFrom <= :now
            order by v.effectiveFrom desc, v.changedAt desc
            """)
    List<SettingValue> findLatestEffective(
            @Param("key") String key,
            @Param("scopeType") ScopeType scopeType,
            @Param("scopeId") UUID scopeId,
            @Param("now") Instant now,
            Pageable pageable);

    default Optional<SettingValue> findLatestEffective(String key, ScopeType scopeType, UUID scopeId, Instant now) {
        return findLatestEffective(key, scopeType, scopeId, now, Pageable.ofSize(1)).stream().findFirst();
    }

    @Query("""
            select v from SettingValue v
            where v.definitionKey = :key
              and v.scopeType = :scopeType
              and ((:scopeId) is null and v.scopeId is null or (:scopeId) is not null and v.scopeId = :scopeId)
            order by v.effectiveFrom desc, v.changedAt desc
            """)
    List<SettingValue> findHistory(
            @Param("key") String key,
            @Param("scopeType") ScopeType scopeType,
            @Param("scopeId") UUID scopeId);
}

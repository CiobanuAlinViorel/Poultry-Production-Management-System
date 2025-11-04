package com.example.shared.domain.repository;

import com.example.shared.domain.entity.Session;
import com.example.shared.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByToken(String token);

    List<Session> findByUser(User user);

    @Query("SELECT s FROM Session s WHERE s.user = :user AND s.isActive = true AND s.expiresAt > :now")
    List<Session> findActiveSessionsByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    default List<Session> findActiveSessionsByUser(User user) {
        return findActiveSessionsByUser(user, LocalDateTime.now());
    }

    @Query("SELECT s FROM Session s WHERE s.expiresAt < :now AND s.isActive = true")
    List<Session> findExpiredSessions(@Param("now") LocalDateTime now);

    void deleteByExpiresAtBefore(LocalDateTime expiryDate);

    long countByUserAndIsActiveTrue(User user);

    @Query("SELECT s FROM Session s WHERE s.ipAddress = :ipAddress AND s.createdAt > :since")
    List<Session> findSessionsByIpAddressSince(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
}
package com.example.identity_service.repositories;

import com.example.identity_service.domain.User;
import com.example.identity_service.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("""
            select u from User u
            where (:status is null or u.status = :status)
              and (lower(u.email) like :pattern
                   or lower(coalesce(u.username, '')) like :pattern
                   or lower(coalesce(u.displayName, '')) like :pattern)
            """)
    Page<User> search(@Param("status") UserStatus status, @Param("pattern") String pattern, Pageable pageable);
}

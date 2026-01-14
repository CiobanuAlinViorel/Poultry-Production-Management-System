package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.FarmEmployee;
import com.example.broilerfarm.domain.entities.FarmUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pentru FarmUser
 *
 * Query methods folosite de sistemul de autentificare și management utilizatori
 */
@Repository
public interface FarmUserRepository extends JpaRepository<FarmUser, Long> {

    /**
     * Găsește user după email (folosit pentru login)
     * @EntityGraph încarcă EAGER rolurile pentru a evita LazyInitializationException
     */
    @EntityGraph(attributePaths = {"roles"})
    Optional<FarmUser> findByEmail(String email);

    /**
     * Găsește user după username
     */
    @EntityGraph(attributePaths = {"roles"})
    Optional<FarmUser> findByUsername(String username);

    /**
     * Găsește user după employee asociat
     */
    Optional<FarmUser> findByEmployee(FarmEmployee employee);

    /**
     * Verifică dacă există user cu email-ul dat
     */
    boolean existsByEmail(String email);

    /**
     * Verifică dacă există user cu username-ul dat
     */
    boolean existsByUsername(String username);
}
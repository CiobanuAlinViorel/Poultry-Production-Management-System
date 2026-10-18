package com.ppms.admin.repositories.catalog;

import com.ppms.admin.domain.catalog.Breed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BreedRepository extends JpaRepository<Breed, UUID> {

    Optional<Breed> findByCode(String code);

    Page<Breed> findByActiveTrue(Pageable pageable);
}

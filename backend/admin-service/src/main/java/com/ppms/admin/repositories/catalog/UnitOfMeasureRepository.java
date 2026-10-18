package com.ppms.admin.repositories.catalog;

import com.ppms.admin.domain.catalog.UnitOfMeasure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, UUID> {

    Optional<UnitOfMeasure> findByCode(String code);

    Page<UnitOfMeasure> findByActiveTrue(Pageable pageable);
}

package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.Consumable;
import com.example.broilerfarm.domain.enums.ConsumableType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsumableRepository extends JpaRepository<Consumable, Long> {

    Optional<Consumable> findByName(String name);

    List<Consumable> findByType(ConsumableType type);

    List<Consumable> findByCategory(String category);

    @Query("SELECT c FROM Consumable c WHERE c.type IN ('FEED_STARTER', 'FEED_GROWER', 'FEED_FINISHER')")
    List<Consumable> findAllFeedTypes();

    @Query("SELECT c FROM Consumable c WHERE c.type IN ('MEDICATION', 'VACCINE')")
    List<Consumable> findAllMedications();

    @Query("SELECT c FROM Consumable c WHERE c.supplier = :supplier")
    List<Consumable> findBySupplier(@Param("supplier") String supplier);

    @Query("SELECT c FROM Consumable c WHERE c.name LIKE %:searchTerm% " +
            "OR c.category LIKE %:searchTerm%")
    List<Consumable> searchByNameOrCategory(@Param("searchTerm") String searchTerm);
}
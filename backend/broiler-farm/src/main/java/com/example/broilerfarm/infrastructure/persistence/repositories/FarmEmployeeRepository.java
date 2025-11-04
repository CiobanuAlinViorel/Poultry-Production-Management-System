package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.FarmEmployee;

import com.example.shared.domain.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmEmployeeRepository extends JpaRepository<FarmEmployee, Long> {

    List<FarmEmployee> findByBroilerFarmId(Long farmId);

    List<FarmEmployee> findByRole(Role role);

    Optional<FarmEmployee> findByEmail(String email);

    @Query("SELECT fe FROM FarmEmployee fe WHERE fe.broilerFarm.id = :farmId AND fe.role = :role")
    List<FarmEmployee> findByFarmIdAndRole(
            @Param("farmId") Long farmId,
            @Param("role") Role role
    );

    @Query("SELECT fe FROM FarmEmployee fe WHERE fe.broilerFarm.id = :farmId " +
            "AND fe.role IN ('FARM_MANAGER', 'LOGISTICS_MANAGER', 'VETERINARIAN')")
    List<FarmEmployee> findManagerialStaffByFarm(@Param("farmId") Long farmId);

    @Query("SELECT fe FROM FarmEmployee fe WHERE fe.broilerFarm.id = :farmId " +
            "AND fe.role = 'WORKER'")
    List<FarmEmployee> findWorkersByFarm(@Param("farmId") Long farmId);

    @Query("SELECT fe FROM FarmEmployee fe WHERE fe.firstName LIKE %:searchTerm% " +
            "OR fe.lastName LIKE %:searchTerm% " +
            "OR fe.email LIKE %:searchTerm%")
    List<FarmEmployee> searchByNameOrEmail(@Param("searchTerm") String searchTerm);

    @Query("SELECT COUNT(fe) FROM FarmEmployee fe WHERE fe.broilerFarm.id = :farmId")
    Long countEmployeesByFarm(@Param("farmId") Long farmId);

    boolean existsByEmail(String email);
}
package com.example.slaughterhouse.infrastructure.persistance.repository;

import com.example.slaughterhouse.domain.entities.ColdStorageLog;
import com.example.slaughterhouse.domain.entities.Package;
import com.example.slaughterhouse.domain.entities.Warehouse;
import com.example.slaughterhouse.domain.enums.StorageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ColdStorageLogRepository extends JpaRepository<ColdStorageLog, Long> {

    // Găsește logul unui pachet specific
    @Query("SELECT c FROM ColdStorageLog c WHERE c.package_ = :packageEntity")
    Optional<ColdStorageLog> findByPackageEntity(@Param("packageEntity") Package packageEntity);

    // Toate logurile active
    List<ColdStorageLog> findByIsActiveTrue();

    // Logurile după status
    List<ColdStorageLog> findByStatus(StorageStatus status);

    // Logurile active dintr-un anumit depozit
    List<ColdStorageLog> findByWarehouseAndIsActiveTrue(Warehouse warehouse);

    // Logurile încă în stoc (nu au exitDate)
    @Query("SELECT c FROM ColdStorageLog c WHERE c.exitDate IS NULL AND c.status = com.example.slaughterhouse.domain.enums.StorageStatus.STORED")
    List<ColdStorageLog> findCurrentlyStored();

    // Ultimul log pentru un anumit pachet
    @Query("SELECT c FROM ColdStorageLog c WHERE c.package_ = :packageEntity ORDER BY c.entryDate DESC, c.entryTime DESC")
    Optional<ColdStorageLog> findLatestByPackage(@Param("packageEntity") Package packageEntity);
}

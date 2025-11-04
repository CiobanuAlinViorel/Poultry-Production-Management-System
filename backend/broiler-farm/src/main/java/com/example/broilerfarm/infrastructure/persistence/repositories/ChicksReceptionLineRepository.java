package com.example.broilerfarm.infrastructure.persistence.repositories;

import com.example.broilerfarm.domain.entities.ChicksReceptionLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChicksReceptionLineRepository extends JpaRepository<ChicksReceptionLine, Long> {

    List<ChicksReceptionLine> findByReceptionId(Long receptionId);

    Optional<ChicksReceptionLine> findByCreatedLotId(Long lotId);

    @Query("SELECT crl FROM ChicksReceptionLine crl WHERE crl.reception.id = :receptionId " +
            "AND crl.poultryHouse.id = :houseId")
    Optional<ChicksReceptionLine> findByReceptionIdAndHouseId(
            @Param("receptionId") Long receptionId,
            @Param("houseId") Long houseId
    );

    @Query("SELECT SUM(crl.chicksAlive) FROM ChicksReceptionLine crl " +
            "WHERE crl.reception.id = :receptionId")
    Integer getTotalChicksAliveByReception(@Param("receptionId") Long receptionId);
}
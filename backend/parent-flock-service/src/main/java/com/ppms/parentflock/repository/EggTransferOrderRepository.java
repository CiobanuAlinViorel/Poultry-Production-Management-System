package com.ppms.parentflock.repository;

import com.ppms.parentflock.domain.EggTransferOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EggTransferOrderRepository extends JpaRepository<EggTransferOrder, UUID> {
}

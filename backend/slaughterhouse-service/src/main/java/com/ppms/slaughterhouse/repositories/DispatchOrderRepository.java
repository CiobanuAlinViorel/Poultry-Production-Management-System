package com.ppms.slaughterhouse.repositories;

import com.ppms.slaughterhouse.domain.DispatchOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DispatchOrderRepository extends JpaRepository<DispatchOrder, UUID> {
}

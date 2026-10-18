package com.ppms.farm.repositories;
import com.ppms.farm.domain.FarmDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface FarmDeliveryRepository extends JpaRepository<FarmDelivery, UUID> { List<FarmDelivery> findAllByLotIdOrderByDispatchDateDesc(UUID lotId); }

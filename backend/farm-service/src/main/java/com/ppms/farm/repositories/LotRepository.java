package com.ppms.farm.repositories;
import com.ppms.farm.domain.Lot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface LotRepository extends JpaRepository<Lot, UUID> { List<Lot> findAllByFarmIdOrderByArrivalDateDesc(UUID farmId); boolean existsByHouseIdAndStatus(UUID houseId, Lot.Status status); }

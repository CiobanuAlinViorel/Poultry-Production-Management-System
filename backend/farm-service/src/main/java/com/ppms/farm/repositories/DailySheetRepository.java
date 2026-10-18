package com.ppms.farm.repositories;
import com.ppms.farm.domain.DailySheet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
public interface DailySheetRepository extends JpaRepository<DailySheet, UUID> { List<DailySheet> findAllByLotIdOrderByDateDesc(UUID lotId); boolean existsByLotIdAndDate(UUID lotId, LocalDate date); }

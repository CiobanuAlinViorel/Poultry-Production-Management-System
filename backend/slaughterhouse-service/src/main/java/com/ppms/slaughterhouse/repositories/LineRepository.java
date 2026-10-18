package com.ppms.slaughterhouse.repositories;

import com.ppms.slaughterhouse.domain.Line;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LineRepository extends JpaRepository<Line, UUID> {

    List<Line> findBySlaughterhouseId(UUID slaughterhouseId);
}

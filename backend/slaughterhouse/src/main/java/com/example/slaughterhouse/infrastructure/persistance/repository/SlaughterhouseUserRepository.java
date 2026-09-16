package com.example.slaughterhouse.infrastructure.persistance.repository;

import com.example.slaughterhouse.domain.entities.SlaughterhouseUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlaughterhouseUserRepository extends JpaRepository<SlaughterhouseUser, Long> {
}

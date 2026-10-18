package com.ppms.admin.repositories;

import com.ppms.admin.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByKey(String key);

    List<Permission> findByKeyIn(Collection<String> keys);
}

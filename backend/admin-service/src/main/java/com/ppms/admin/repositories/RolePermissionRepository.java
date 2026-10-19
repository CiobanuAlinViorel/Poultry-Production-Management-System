package com.ppms.admin.repositories;

import com.ppms.admin.domain.RolePermission;
import com.ppms.admin.domain.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findByRoleId(UUID roleId);

    List<RolePermission> findByRoleIdIn(Collection<UUID> roleIds);

    void deleteByRoleId(UUID roleId);
}

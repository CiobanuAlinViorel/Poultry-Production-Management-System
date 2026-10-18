package com.ppms.admin.repositories;

import com.ppms.admin.domain.UserRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, UUID> {

    List<UserRoleAssignment> findByUserId(UUID userId);

    List<UserRoleAssignment> findByUserIdIn(Collection<UUID> userIds);

    long countByRoleIdAndRevokedAtIsNull(UUID roleId);
}

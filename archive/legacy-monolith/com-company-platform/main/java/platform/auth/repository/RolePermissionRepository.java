package com.company.platform.auth.repository;

import com.company.platform.auth.entity.RolePermission;
import com.company.platform.auth.entity.id.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    boolean existsByIdRoleIdAndIdPermissionId(UUID roleId, UUID permissionId);
}

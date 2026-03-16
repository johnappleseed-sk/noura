package com.noura.platform.repository;

import com.noura.platform.domain.entity.AdminRolePermission;
import com.noura.platform.domain.entity.id.AdminRolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Module: Admin RBAC Persistence
 * Purpose: Data access for admin role-permission assignment rows.
 * Responsibilities:
 * - Fetch role permissions with permission details for matrix/capability resolution.
 * - Replace role grants during permission assignment workflows.
 */
public interface AdminRolePermissionRepository extends JpaRepository<AdminRolePermission, AdminRolePermissionId> {

    @Query("""
            select rolePermission from AdminRolePermission rolePermission
            join fetch rolePermission.role role
            join fetch rolePermission.permission permission
            where role.id in :roleIds
            """)
    List<AdminRolePermission> findDetailedByRoleIds(@Param("roleIds") Collection<UUID> roleIds);

    @Modifying
    @Query("""
            delete from AdminRolePermission rolePermission
            where rolePermission.role.id = :roleId
            """)
    void deleteByRoleId(@Param("roleId") UUID roleId);
}

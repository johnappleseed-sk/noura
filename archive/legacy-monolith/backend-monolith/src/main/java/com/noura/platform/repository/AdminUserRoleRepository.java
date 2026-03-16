package com.noura.platform.repository;

import com.noura.platform.domain.entity.AdminUserRole;
import com.noura.platform.domain.entity.id.AdminUserRoleId;
import com.noura.platform.repository.projection.AdminRoleAssignmentCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Module: Admin RBAC Persistence
 * Purpose: Data access for assigning admin roles to platform users.
 * Responsibilities:
 * - Fetch user role assignments with role + permission details for auth resolution.
 * - Replace user role assignments in admin workflows.
 * - Provide per-role assignment counts for role management summaries.
 */
public interface AdminUserRoleRepository extends JpaRepository<AdminUserRole, AdminUserRoleId> {

    @Query("""
            select distinct userRole from AdminUserRole userRole
            join fetch userRole.role role
            left join fetch role.rolePermissions rolePermission
            left join fetch rolePermission.permission permission
            where userRole.user.id = :userId
              and role.active = true
            """)
    List<AdminUserRole> findDetailedByUserId(@Param("userId") UUID userId);

    @Query("""
            select distinct userRole from AdminUserRole userRole
            join fetch userRole.role role
            where userRole.user.id in :userIds
              and role.active = true
            """)
    List<AdminUserRole> findDetailedByUserIds(@Param("userIds") Collection<UUID> userIds);

    @Modifying
    @Query("""
            delete from AdminUserRole userRole
            where userRole.user.id = :userId
            """)
    void deleteByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("""
            delete from AdminUserRole userRole
            where userRole.user.id in :userIds
            """)
    void deleteByUserIds(@Param("userIds") Collection<UUID> userIds);

    @Modifying
    @Query("""
            delete from AdminUserRole userRole
            where userRole.role.id = :roleId
            """)
    void deleteByRoleId(@Param("roleId") UUID roleId);

    @Query("""
            select userRole.role.id as roleId, count(userRole) as userCount
            from AdminUserRole userRole
            where userRole.role.id in :roleIds
            group by userRole.role.id
            """)
    List<AdminRoleAssignmentCountProjection> countByRoleIds(@Param("roleIds") Collection<UUID> roleIds);
}

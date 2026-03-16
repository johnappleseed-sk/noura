package com.noura.platform.domain.entity;

import com.noura.platform.domain.entity.id.AdminRolePermissionId;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Module: Admin RBAC Persistence
 * Purpose: Join entity for admin role-to-permission assignments.
 * Responsibilities:
 * - Persist which permissions are granted to a role.
 * Related modules:
 * - AdminRole
 * - AdminPermission
 * - AdminRoleManagementService
 */
@Getter
@Setter
@Entity
@Table(name = "admin_role_permissions")
public class AdminRolePermission {

    @EmbeddedId
    private AdminRolePermissionId id;

    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private AdminRole role;

    @MapsId("permissionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private AdminPermission permission;
}

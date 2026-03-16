package com.noura.platform.domain.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Module: Admin RBAC Persistence
 * Purpose: Composite identifier for admin role-permission assignment rows.
 * Responsibilities:
 * - Represent `(role_id, permission_id)` as the primary key for admin role permissions.
 * Related modules:
 * - AdminRolePermission
 * - AdminRole
 * - AdminPermission
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class AdminRolePermissionId implements Serializable {

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;
}

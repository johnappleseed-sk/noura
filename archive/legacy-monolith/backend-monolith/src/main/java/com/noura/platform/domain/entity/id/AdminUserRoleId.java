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
 * Purpose: Composite identifier for admin user-role assignment rows.
 * Responsibilities:
 * - Represent `(user_id, role_id)` as the primary key for admin user role assignments.
 * Related modules:
 * - AdminUserRole
 * - UserAccount
 * - AdminRole
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class AdminUserRoleId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;
}

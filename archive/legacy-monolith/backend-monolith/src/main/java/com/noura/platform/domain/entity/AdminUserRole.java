package com.noura.platform.domain.entity;

import com.noura.platform.domain.entity.id.AdminUserRoleId;
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
 * Purpose: Join entity for assigning admin roles to platform user accounts.
 * Responsibilities:
 * - Persist user-to-admin-role assignments used by runtime authorization.
 * Related modules:
 * - UserAccount
 * - AdminRole
 * - CustomUserDetailsService
 */
@Getter
@Setter
@Entity
@Table(name = "admin_user_roles")
public class AdminUserRole {

    @EmbeddedId
    private AdminUserRoleId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private AdminRole role;
}

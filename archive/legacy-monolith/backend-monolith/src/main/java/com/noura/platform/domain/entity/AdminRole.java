package com.noura.platform.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Module: Admin RBAC Persistence
 * Purpose: Stores admin role definitions used for capability and permission governance.
 * Responsibilities:
 * - Persist role metadata (code, lifecycle flags, assignability).
 * - Own role-permission and user-role assignments.
 * Related modules:
 * - AdminRolePermission
 * - AdminUserRole
 * - AdminRoleManagementService
 */
@Getter
@Setter
@Entity
@Table(name = "admin_roles")
public class AdminRole extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(length = 600)
    private String description;

    @Column(name = "is_system_role", nullable = false)
    private boolean systemRole = false;

    @Column(nullable = false)
    private boolean assignable = true;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AdminRolePermission> rolePermissions = new HashSet<>();
}

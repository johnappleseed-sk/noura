package com.noura.platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Module: Admin RBAC Persistence
 * Purpose: Stores normalized permission definitions used by admin role grants.
 * Responsibilities:
 * - Persist scope/action permission primitives.
 * - Provide stable identifiers for role-permission assignment workflows.
 * Related modules:
 * - AdminRolePermission
 * - AdminRoleManagementService
 * - AdminAuthorizationServiceImpl
 */
@Getter
@Setter
@Entity
@Table(name = "admin_permissions")
public class AdminPermission extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String scope;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(length = 600)
    private String description;

    @Column(name = "module_group", nullable = false, length = 80)
    private String moduleGroup = "governance";

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1000;

    @Column(name = "is_sensitive", nullable = false)
    private boolean sensitive = false;
}

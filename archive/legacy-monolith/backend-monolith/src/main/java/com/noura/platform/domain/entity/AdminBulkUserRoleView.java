package com.noura.platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Module: Admin Authorization
 * Purpose: Stores reusable bulk user-role assignment views for admin workflows.
 * Responsibilities:
 * - Persist saved bulk assignment filters and selected role drafts per actor.
 * - Provide server-backed view state for admin governance UX.
 * Related modules:
 * - AdminRoleManagementService
 * - RolesPermissionsPage
 */
@Getter
@Setter
@Entity
@Table(name = "admin_bulk_user_role_views")
public class AdminBulkUserRoleView extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "query_text", length = 255)
    private String queryText;

    @Lob
    @Column(name = "user_ids_json", nullable = false, columnDefinition = "TEXT")
    private String userIdsJson = "[]";

    @Lob
    @Column(name = "role_codes_json", nullable = false, columnDefinition = "TEXT")
    private String roleCodesJson = "[]";
}

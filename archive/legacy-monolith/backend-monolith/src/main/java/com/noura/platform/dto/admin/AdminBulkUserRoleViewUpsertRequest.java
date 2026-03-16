package com.noura.platform.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Module: Admin Authorization
 * Purpose: Request payload for creating or updating a saved bulk assignment view.
 * Responsibilities:
 * - Capture view name, optional query text, and selected user/role drafts.
 * Related modules:
 * - AdminAuthorizationController
 * - AdminRoleManagementService
 */
public record AdminBulkUserRoleViewUpsertRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 255) String query,
        @Size(max = 200) List<UUID> userIds,
        @Size(max = 30) List<String> roleCodes
) {
}

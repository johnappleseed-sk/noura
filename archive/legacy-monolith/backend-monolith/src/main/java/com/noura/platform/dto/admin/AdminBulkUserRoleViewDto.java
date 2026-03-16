package com.noura.platform.dto.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Module: Admin Authorization
 * Purpose: Represents a persisted bulk user-role assignment view.
 * Responsibilities:
 * - Return actor-scoped saved bulk assignment filters and drafts to admin UI.
 * Related modules:
 * - AdminRoleManagementService
 * - RolesPermissionsPage
 */
public record AdminBulkUserRoleViewDto(
        UUID id,
        String name,
        String query,
        List<UUID> userIds,
        List<String> roleCodes,
        Instant updatedAt
) {
}

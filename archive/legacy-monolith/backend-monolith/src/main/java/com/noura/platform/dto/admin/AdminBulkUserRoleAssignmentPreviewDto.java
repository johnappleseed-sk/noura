package com.noura.platform.dto.admin;

import java.util.List;
import java.util.UUID;

/**
 * Module: Admin Authorization
 * Purpose: Returns preview summary for bulk role-assignment operations.
 * Responsibilities:
 * - Report missing user ids and affected-user counters.
 * - Return per-user role diffs for conflict inspection prior to apply.
 * Related modules:
 * - AdminAuthorizationController
 * - frontend RolesPermissionsPage
 */
public record AdminBulkUserRoleAssignmentPreviewDto(
        int requestedUsers,
        int resolvableUsers,
        int missingUsers,
        int changedUsers,
        List<UUID> missingUserIds,
        List<AdminBulkUserRoleAssignmentPreviewItemDto> items
) {
}

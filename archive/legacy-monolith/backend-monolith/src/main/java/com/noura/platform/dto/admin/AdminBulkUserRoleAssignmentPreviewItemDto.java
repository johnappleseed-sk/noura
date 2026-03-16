package com.noura.platform.dto.admin;

import java.util.List;
import java.util.UUID;

/**
 * Module: Admin Authorization
 * Purpose: Represents per-user diff details for a bulk role-assignment preview.
 * Responsibilities:
 * - Provide current and proposed role sets for one user.
 * - Expose additive and removal deltas used by admin conflict review UI.
 * Related modules:
 * - AdminRoleManagementService
 * - frontend RolesPermissionsPage
 */
public record AdminBulkUserRoleAssignmentPreviewItemDto(
        UUID userId,
        String email,
        String fullName,
        List<String> currentRoleCodes,
        List<String> proposedRoleCodes,
        List<String> rolesToAdd,
        List<String> rolesToRemove,
        boolean changed
) {
}

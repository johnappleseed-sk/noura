package com.noura.platform.dto.admin;

import java.util.List;

/**
 * Module: Admin Authorization
 * Purpose: Returns summary and per-user data for bulk user-role replacement jobs.
 * Responsibilities:
 * - Report requested and updated user counts for admin feedback.
 * - Return normalized assignment results for each processed user.
 * Related modules:
 * - AdminAuthorizationController
 * - frontend RolesPermissionsPage
 */
public record AdminBulkUserRoleAssignmentResultDto(
        int requestedUsers,
        int updatedUsers,
        List<AdminUserRoleAssignmentDto> assignments
) {
}

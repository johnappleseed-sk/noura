package com.noura.platform.dto.admin;

import java.util.List;
import java.util.UUID;

/**
 * Module: Admin Authorization
 * Purpose: Response payload for user-role assignment operations.
 * Responsibilities:
 * - Expose target user metadata and assigned admin role codes.
 * - Keep legacy platform roles visible for transition compatibility.
 * Related modules:
 * - AdminRoleManagementService
 * - AdminAuthorizationController
 */
public record AdminUserRoleAssignmentDto(
        UUID userId,
        String email,
        String fullName,
        List<String> adminRoleCodes,
        List<String> platformRoles
) {
}

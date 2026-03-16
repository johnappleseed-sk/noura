package com.noura.platform.dto.admin;

import java.util.UUID;

/**
 * Module: Admin Authorization
 * Purpose: Describes a persisted permission catalog entry.
 * Responsibilities:
 * - Expose scope/action permission metadata for admin role assignment UI.
 * Related modules:
 * - AdminRoleManagementService
 * - AdminAuthorizationController
 */
public record AdminPermissionDto(
        UUID id,
        String scope,
        String action,
        String label,
        String description,
        String moduleGroup,
        int displayOrder,
        boolean sensitive
) {
}

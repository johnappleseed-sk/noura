package com.noura.platform.dto.admin;

import java.util.List;
import java.util.Map;

/**
 * Module: Admin Authorization
 * Purpose: Represents a reusable permission preset template for role grant workflows.
 * Responsibilities:
 * - Expose preset metadata and grants to admin UI.
 * - Support explicit preset application onto mutable roles.
 * Related modules:
 * - AdminRoleManagementService
 * - frontend RolesPermissionsPage
 */
public record AdminPermissionPresetDto(
        String code,
        String label,
        String description,
        int moduleCount,
        int permissionCount,
        Map<String, List<String>> grants
) {
}

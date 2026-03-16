package com.noura.platform.dto.admin;

import java.util.List;

/**
 * Module: Admin Authorization
 * Purpose: Response envelope for the enterprise role-permission matrix API.
 * Responsibilities:
 * - Provide a versioned matrix contract for admin RBAC UI consumers.
 * - Expose global action catalog, scope catalog, and role grants in one payload.
 * Related modules:
 * - AdminPermissionScopeDto
 * - AdminRolePermissionDto
 * - AdminAuthorizationService
 */
public record AdminAuthorizationMatrixDto(
        String version,
        List<String> actionCatalog,
        List<AdminPermissionScopeDto> scopes,
        List<AdminRolePermissionDto> roles
) {
}

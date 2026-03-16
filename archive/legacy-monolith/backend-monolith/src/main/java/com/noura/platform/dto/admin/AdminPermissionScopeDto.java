package com.noura.platform.dto.admin;

import java.util.List;

/**
 * Module: Admin Authorization
 * Purpose: Represents a permission scope definition for the admin RBAC matrix API.
 * Responsibilities:
 * - Describe a domain scope code, label, and supported actions.
 * - Provide a frontend-friendly shape for permission matrix rendering.
 * Related modules:
 * - AdminAuthorizationMatrixDto
 * - AdminAuthorizationService
 * - AdminDashboardController
 */
public record AdminPermissionScopeDto(
        String scope,
        String label,
        String description,
        List<String> supportedActions
) {
}

package com.noura.platform.dto.admin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Module: Admin Authorization
 * Purpose: Represents one role row in the admin RBAC matrix payload.
 * Responsibilities:
 * - Describe role identity and lifecycle flags for the admin permission catalog.
 * - Expose scope-action grants for matrix rendering and policy inspection.
 * - Expose role-derived UI capabilities used by the admin dashboard.
 * Related modules:
 * - AdminAuthorizationMatrixDto
 * - AdminAuthorizationService
 */
public record AdminRolePermissionDto(
        UUID id,
        String role,
        String label,
        String description,
        boolean systemRole,
        boolean assignable,
        boolean activeInRuntime,
        long assignedUsers,
        Map<String, List<String>> grants,
        List<String> capabilities
) {
}

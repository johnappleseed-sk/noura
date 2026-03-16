package com.noura.platform.dto.admin;

import jakarta.validation.constraints.Size;

/**
 * Module: Admin Authorization
 * Purpose: Request payload for updating mutable admin role metadata.
 * Responsibilities:
 * - Update role label/description and lifecycle flags.
 * Related modules:
 * - AdminRoleManagementService
 * - AdminAuthorizationController
 */
public record AdminRoleUpdateRequest(
        @Size(max = 255) String label,
        @Size(max = 600) String description,
        Boolean assignable,
        Boolean active
) {
}

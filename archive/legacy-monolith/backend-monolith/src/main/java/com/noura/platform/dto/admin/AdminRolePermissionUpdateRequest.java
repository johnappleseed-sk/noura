package com.noura.platform.dto.admin;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Module: Admin Authorization
 * Purpose: Request payload for replacing all grants assigned to a role.
 * Responsibilities:
 * - Capture scope-to-actions mapping used by permission assignment workflows.
 * Related modules:
 * - AdminRoleManagementService
 * - AdminAuthorizationController
 */
public record AdminRolePermissionUpdateRequest(
        @NotNull Map<String, List<String>> grants
) {
}

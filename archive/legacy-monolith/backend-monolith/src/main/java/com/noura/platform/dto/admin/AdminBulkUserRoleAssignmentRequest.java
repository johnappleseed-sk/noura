package com.noura.platform.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Module: Admin Authorization
 * Purpose: Defines a bulk user-role replacement payload for governance workflows.
 * Responsibilities:
 * - Capture a bounded set of target users for batch role replacement.
 * - Carry the role-code set that should be applied to each target user.
 * Related modules:
 * - AdminAuthorizationController
 * - AdminRoleManagementService
 */
public record AdminBulkUserRoleAssignmentRequest(
        @NotEmpty(message = "userIds is required")
        @Size(max = 200, message = "userIds cannot exceed 200 entries")
        List<UUID> userIds,
        @Size(max = 30, message = "roleCodes cannot exceed 30 entries")
        List<String> roleCodes
) {
}

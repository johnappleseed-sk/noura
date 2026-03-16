package com.noura.platform.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Module: Admin Authorization
 * Purpose: Request payload for assigning admin roles to a user account.
 * Responsibilities:
 * - Capture normalized admin role codes to replace for a target user.
 * Related modules:
 * - AdminRoleManagementService
 * - AdminAuthorizationController
 */
public record AdminUserRoleAssignmentRequest(
        @NotNull @Size(max = 100) List<String> roleCodes
) {
}

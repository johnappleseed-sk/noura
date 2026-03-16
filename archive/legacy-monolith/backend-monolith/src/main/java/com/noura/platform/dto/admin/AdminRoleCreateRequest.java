package com.noura.platform.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * Module: Admin Authorization
 * Purpose: Request payload for creating a new admin role.
 * Responsibilities:
 * - Capture normalized role metadata and optional initial grants.
 * Related modules:
 * - AdminRoleManagementService
 * - AdminAuthorizationController
 */
public record AdminRoleCreateRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Z0-9_]+$") String code,
        @NotBlank @Size(max = 255) String label,
        @Size(max = 600) String description,
        Boolean assignable,
        Boolean active,
        Map<String, List<String>> grants
) {
}

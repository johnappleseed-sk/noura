package com.noura.platform.service;

import com.noura.platform.dto.admin.AdminAuthorizationMatrixDto;

import java.util.Map;
import java.util.Set;

/**
 * Module: Admin Authorization
 * Purpose: Defines the centralized policy contract for admin RBAC capabilities and matrix data.
 * Responsibilities:
 * - Provide a versioned role-permission matrix for admin workflows.
 * - Resolve UI capabilities from normalized runtime roles.
 * - Publish the known role catalog for role normalization in controllers.
 * Related modules:
 * - AdminDashboardController
 * - AdminAuthorizationMatrixDto
 */
public interface AdminAuthorizationService {

    /**
     * Builds the complete admin role-permission matrix payload.
     *
     * @return The matrix contract used by admin UI surfaces.
     */
    AdminAuthorizationMatrixDto matrix();

    /**
     * Resolves dashboard capability flags for the supplied normalized role set.
     *
     * @param roles Normalized role codes (without ROLE_ prefix).
     * @return A deterministic capability map keyed by capability id.
     */
    Map<String, Boolean> capabilitiesForRoles(Set<String> roles);

    /**
     * Returns the role codes recognized by the authorization policy catalog.
     *
     * @return Immutable role code set.
     */
    Set<String> knownRoleCodes();
}

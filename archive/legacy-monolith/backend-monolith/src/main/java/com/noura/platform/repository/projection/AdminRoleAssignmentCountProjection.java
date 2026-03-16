package com.noura.platform.repository.projection;

import java.util.UUID;

/**
 * Module: Admin RBAC Persistence
 * Purpose: Projection for counting user assignments per admin role.
 * Responsibilities:
 * - Expose role id and assignment count pair for role list summaries.
 * Related modules:
 * - AdminUserRoleRepository
 * - AdminRoleManagementServiceImpl
 */
public interface AdminRoleAssignmentCountProjection {

    UUID getRoleId();

    long getUserCount();
}

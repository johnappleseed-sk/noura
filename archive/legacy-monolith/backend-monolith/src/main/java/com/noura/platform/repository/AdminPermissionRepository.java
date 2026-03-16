package com.noura.platform.repository;

import com.noura.platform.domain.entity.AdminPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Module: Admin RBAC Persistence
 * Purpose: Data access for admin permission catalog entries.
 * Responsibilities:
 * - Resolve scope/action permission records for grant assignments.
 * - List permission catalog entries in deterministic order for UI workflows.
 */
public interface AdminPermissionRepository extends JpaRepository<AdminPermission, UUID> {

    Optional<AdminPermission> findByScopeIgnoreCaseAndActionIgnoreCase(String scope, String action);

    List<AdminPermission> findAllByOrderByDisplayOrderAscScopeAscActionAsc();

    List<AdminPermission> findAllByOrderByScopeAscActionAsc();
}

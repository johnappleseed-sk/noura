package com.noura.platform.repository;

import com.noura.platform.domain.entity.AdminBulkUserRoleView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Module: Admin Authorization
 * Purpose: Data access for persisted bulk user-role assignment views.
 * Responsibilities:
 * - Read and write actor-scoped bulk assignment views.
 * - Resolve views by owner/name and by owner/id for secure mutation workflows.
 */
public interface AdminBulkUserRoleViewRepository extends JpaRepository<AdminBulkUserRoleView, UUID> {

    List<AdminBulkUserRoleView> findAllByOwnerUserIdOrderByNameAsc(UUID ownerUserId);

    Optional<AdminBulkUserRoleView> findByOwnerUserIdAndNameIgnoreCase(UUID ownerUserId, String name);

    Optional<AdminBulkUserRoleView> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
}

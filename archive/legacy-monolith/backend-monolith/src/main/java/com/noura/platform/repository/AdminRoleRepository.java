package com.noura.platform.repository;

import com.noura.platform.domain.entity.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Module: Admin RBAC Persistence
 * Purpose: Data access for admin role definitions.
 * Responsibilities:
 * - Resolve roles by code and lifecycle flags.
 * - Support role CRUD and assignment workflows.
 */
public interface AdminRoleRepository extends JpaRepository<AdminRole, UUID> {

    Optional<AdminRole> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<AdminRole> findAllByOrderByCodeAsc();

    List<AdminRole> findAllBySystemRoleTrueAndActiveTrueOrderByCodeAsc();

    @Query("""
            select role from AdminRole role
            where upper(role.code) in :codes
            """)
    List<AdminRole> findAllByCodes(@Param("codes") Collection<String> codes);

    @Query("""
            select role.code from AdminRole role
            where role.active = true
            order by role.code asc
            """)
    List<String> findActiveCodes();
}

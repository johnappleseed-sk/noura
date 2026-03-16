package com.noura.platform.repository;

import com.noura.platform.domain.entity.AdminRbacAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Module: Admin Authorization
 * Purpose: Data access for RBAC governance audit-log persistence and queries.
 * Responsibilities:
 * - Store mutation audit events for roles and assignments.
 * - Support filtered pageable audit-log queries for admin workflows.
 */
public interface AdminRbacAuditLogRepository extends JpaRepository<AdminRbacAuditLog, UUID>, JpaSpecificationExecutor<AdminRbacAuditLog> {
}


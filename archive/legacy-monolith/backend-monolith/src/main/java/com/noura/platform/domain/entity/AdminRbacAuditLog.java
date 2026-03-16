package com.noura.platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Module: Admin Authorization
 * Purpose: Captures immutable RBAC governance mutation events.
 * Responsibilities:
 * - Persist actor, action, target, and payload metadata for role and assignment changes.
 * - Support admin-facing audit-log review workflows.
 * Related modules:
 * - AdminRoleManagementServiceImpl
 * - AdminRbacAuditLogRepository
 */
@Getter
@Setter
@Entity
@Table(name = "admin_rbac_audit_logs")
public class AdminRbacAuditLog extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String actionType;

    @Column(nullable = false, length = 80)
    private String entityType;

    @Column(length = 120)
    private String entityId;

    @Column(length = 255)
    private String actorEmail;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(nullable = false, length = 40)
    private String outcome;

    @Column(length = 120)
    private String correlationId;

    @Column(nullable = false, length = 64)
    private String payloadHash;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String detailsJson;

    @Column(nullable = false)
    private Instant occurredAt;
}

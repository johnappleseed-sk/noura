package com.noura.platform.dto.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * Module: Admin Authorization
 * Purpose: Represents one RBAC governance audit-log row returned to admin clients.
 * Responsibilities:
 * - Expose actor, action, target, and metadata payload for role/assignment mutations.
 * Related modules:
 * - AdminRoleManagementService
 * - AdminAuthorizationController
 */
public record AdminRbacAuditLogDto(
        UUID id,
        String actionType,
        String entityType,
        String entityId,
        String actorEmail,
        UUID actorUserId,
        String outcome,
        String correlationId,
        String payloadHash,
        String detailsJson,
        Instant occurredAt
) {
}

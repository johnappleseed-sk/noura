package com.noura.platform.inventory.dto.audit;

import java.time.Instant;

public record AuditLogResponse(
        String id,
        String actorUserId,
        String actorEmail,
        String actionCode,
        String entityType,
        String entityId,
        String correlationId,
        String beforeStateJson,
        String afterStateJson,
        String metadataJson,
        String ipAddress,
        String userAgent,
        String eventHash,
        Instant occurredAt
) {
}

package com.noura.platform.dto.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorUserId,
        String actorUsername,
        String actionCode,
        String entityType,
        UUID entityId,
        String oldValueJson,
        String newValueJson,
        String requestPath,
        String requestMethod,
        String ipAddress,
        Instant createdAt
) {
}

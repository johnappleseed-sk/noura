package com.noura.platform.inventory.dto.audit;

import java.time.Instant;

public record AuditLogFilter(
        String entityType,
        String entityId,
        String actionCode,
        String actorEmail,
        Instant occurredFrom,
        Instant occurredTo
) {
}

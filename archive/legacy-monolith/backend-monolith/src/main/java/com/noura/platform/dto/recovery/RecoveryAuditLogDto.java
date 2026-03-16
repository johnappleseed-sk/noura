package com.noura.platform.dto.recovery;

import com.noura.platform.domain.enums.RecoveryActionType;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an immutable recovery audit event returned to the admin recovery center.
 *
 * @param id The audit-log identifier.
 * @param tenantKey The tenant scope.
 * @param entityType The business entity type.
 * @param entityId The business entity identifier.
 * @param actionType The governed action.
 * @param actionStatus The final action status.
 * @param actor The operator that executed the action.
 * @param correlationId The correlated request identifier.
 * @param message The audit message.
 * @param metadataJson The recorded metadata payload.
 * @param occurredAt The occurrence timestamp.
 */
public record RecoveryAuditLogDto(
        UUID id,
        String tenantKey,
        String entityType,
        String entityId,
        RecoveryActionType actionType,
        String actionStatus,
        String actor,
        String correlationId,
        String message,
        String metadataJson,
        Instant occurredAt
) {
}

package com.noura.platform.event;

import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.domain.enums.RecoveryLifecycleState;

import java.time.Instant;

/**
 * Represents a domain event emitted by the recovery governance layer.
 *
 * @param tenantKey The tenant scope.
 * @param entityType The business entity type.
 * @param entityId The business entity identifier.
 * @param actionType The governed action.
 * @param lifecycleState The resulting lifecycle state.
 * @param actor The operator responsible for the action.
 * @param occurredAt The occurrence timestamp.
 * @param successful Whether the action completed successfully.
 */
public record RecoveryDomainEvent(
        String tenantKey,
        String entityType,
        String entityId,
        RecoveryActionType actionType,
        RecoveryLifecycleState lifecycleState,
        String actor,
        Instant occurredAt,
        boolean successful
) {
}

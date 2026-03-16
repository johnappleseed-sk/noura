package com.noura.platform.dto.recovery;

import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.domain.enums.RecoveryJobStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an orchestrated bulk destructive-action job returned to the admin recovery center.
 *
 * @param id The job identifier.
 * @param tenantKey The tenant scope.
 * @param entityType The business entity type.
 * @param actionType The governed action.
 * @param status The current job status.
 * @param requestedBy The operator that submitted the job.
 * @param dryRun Whether the job only validates input.
 * @param totalItems The total requested items.
 * @param processedItems The processed item count.
 * @param successItems The successful item count.
 * @param failedItems The failed item count.
 * @param startedAt The job start timestamp.
 * @param completedAt The job completion timestamp.
 * @param validationSummaryJson The validation summary payload.
 * @param resultSummaryJson The result summary payload.
 * @param errorSummary The terminal error summary, when present.
 * @param updatedAt The last update timestamp.
 */
public record RecoveryActionJobDto(
        UUID id,
        String tenantKey,
        String entityType,
        RecoveryActionType actionType,
        RecoveryJobStatus status,
        String requestedBy,
        boolean dryRun,
        Integer totalItems,
        Integer processedItems,
        Integer successItems,
        Integer failedItems,
        Instant startedAt,
        Instant completedAt,
        String validationSummaryJson,
        String resultSummaryJson,
        String errorSummary,
        Instant updatedAt
) {
}

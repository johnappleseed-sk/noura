package com.noura.platform.service.recovery;

import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.domain.enums.RecoveryJobStatus;
import com.noura.platform.domain.enums.RecoveryLifecycleState;
import com.noura.platform.dto.recovery.RecoveryActionJobDto;
import com.noura.platform.dto.recovery.RecoveryActionRequest;
import com.noura.platform.dto.recovery.RecoveryActionResultDto;
import com.noura.platform.dto.recovery.RecoveryAuditLogDto;
import com.noura.platform.dto.recovery.RecoveryBulkActionRequest;
import com.noura.platform.dto.recovery.RecoveryRecordDto;
import com.noura.platform.dto.recovery.RecoveryVersionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Defines the reusable destructive-action governance service contract.
 */
public interface RecoveryGovernanceService {

    /**
     * Lists governed recovery records for the current tenant scope.
     *
     * @param entityType The optional business entity type filter.
     * @param lifecycleState The optional lifecycle-state filter.
     * @param query The optional free-text filter.
     * @param pageable The pagination configuration.
     * @return A page of governed recovery records.
     */
    Page<RecoveryRecordDto> listRecords(String entityType, RecoveryLifecycleState lifecycleState, String query, Pageable pageable);

    /**
     * Lists recovery versions for the requested governed entity.
     *
     * @param entityType The business entity type.
     * @param entityId The business entity identifier.
     * @return The ordered recovery versions.
     */
    List<RecoveryVersionDto> listVersions(String entityType, String entityId);

    /**
     * Lists recovery audit logs for the current tenant scope.
     *
     * @param entityType The optional business entity type filter.
     * @param actionType The optional action-type filter.
     * @param actionStatus The optional action-status filter.
     * @param query The optional free-text filter.
     * @param errorsOnly Whether to only return non-successful events.
     * @param pageable The pagination configuration.
     * @return A page of audit-log entries.
     */
    Page<RecoveryAuditLogDto> listAuditLogs(
            String entityType,
            RecoveryActionType actionType,
            String actionStatus,
            String query,
            Boolean errorsOnly,
            Pageable pageable
    );

    /**
     * Lists bulk destructive-action jobs for the current tenant scope.
     *
     * @param entityType The optional business entity type filter.
     * @param status The optional job-status filter.
     * @param query The optional free-text filter.
     * @param errorsOnly Whether to only return jobs with errors/failures.
     * @param pageable The pagination configuration.
     * @return A page of orchestrated recovery jobs.
     */
    Page<RecoveryActionJobDto> listJobs(
            String entityType,
            RecoveryJobStatus status,
            String query,
            Boolean errorsOnly,
            Pageable pageable
    );

    /**
     * Executes a single governed destructive or recovery action in the current tenant scope.
     *
     * @param request The requested action payload.
     * @param actor The authenticated actor name.
     * @return The action result.
     */
    RecoveryActionResultDto applyAction(RecoveryActionRequest request, String actor);

    /**
     * Enqueues a governed bulk destructive-action job in the current tenant scope.
     *
     * @param request The bulk action payload.
     * @param actor The authenticated actor name.
     * @return The created bulk-action job.
     */
    RecoveryActionJobDto submitBulkAction(RecoveryBulkActionRequest request, String actor);

    /**
     * Requests cancellation of a queued or running bulk-action job.
     *
     * @param jobId The job identifier.
     * @param actor The authenticated actor name.
     * @return The updated job DTO.
     */
    RecoveryActionJobDto requestJobCancel(UUID jobId, String actor);

    /**
     * Retries a previous job by creating a new job request from stored payload.
     *
     * @param jobId The job identifier to retry.
     * @param failedOnly Whether to only retry failed items when available.
     * @param actor The authenticated actor name.
     * @return The new queued job DTO.
     */
    RecoveryActionJobDto retryJob(UUID jobId, boolean failedOnly, String actor);

    /**
     * Builds a CSV failure report for a completed job.
     *
     * @param jobId The job identifier.
     * @return CSV string content.
     */
    String failureReportCsv(UUID jobId);

    /**
     * Captures a version snapshot for a governed entity without changing lifecycle state.
     *
     * @param entityType The business entity type.
     * @param entityId The business entity identifier.
     * @param actionType The action associated with the version.
     * @param actor The authenticated actor name.
     * @param reason The operator-facing reason for the capture.
     * @param metadata Optional structured metadata recorded with the capture.
     */
    void captureVersion(String entityType, String entityId, RecoveryActionType actionType, String actor, String reason, Map<String, Object> metadata);

    /**
     * Resolves governed lifecycle states for a collection of entity ids.
     *
     * @param entityType The business entity type.
     * @param entityIds The business entity identifiers.
     * @return A map keyed by entity id with current lifecycle states.
     */
    Map<String, RecoveryLifecycleState> resolveLifecycleStates(String entityType, Collection<String> entityIds);
}

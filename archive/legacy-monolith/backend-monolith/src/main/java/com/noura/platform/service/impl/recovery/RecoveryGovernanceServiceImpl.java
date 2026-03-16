package com.noura.platform.service.impl.recovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.ForbiddenException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.config.RecoveryProperties;
import com.noura.platform.domain.entity.RecoveryActionJob;
import com.noura.platform.domain.entity.RecoveryAuditLog;
import com.noura.platform.domain.entity.RecoveryRecord;
import com.noura.platform.domain.entity.RecoveryVersion;
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
import com.noura.platform.event.RecoveryDomainEvent;
import com.noura.platform.repository.RecoveryActionJobRepository;
import com.noura.platform.repository.RecoveryAuditLogRepository;
import com.noura.platform.repository.RecoveryRecordRepository;
import com.noura.platform.repository.RecoveryVersionRepository;
import com.noura.platform.service.recovery.RecoverableEntityAdapter;
import com.noura.platform.service.recovery.RecoverableEntityHandle;
import com.noura.platform.service.recovery.RecoverableEntityRegistry;
import com.noura.platform.service.recovery.RecoveryGovernanceService;
import com.noura.platform.service.recovery.RecoveryMetricsRecorder;
import com.noura.platform.service.recovery.TenantScopeResolver;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implements the reusable destructive-action governance service.
 */
@Service
public class RecoveryGovernanceServiceImpl implements RecoveryGovernanceService {
    private static final Set<RecoveryLifecycleState> TERMINAL_STATES = Set.of(
            RecoveryLifecycleState.PURGED,
            RecoveryLifecycleState.ANONYMIZED
    );

    private static final Set<RecoveryActionType> DESTRUCTIVE_ACTIONS = Set.of(
            RecoveryActionType.TRASH,
            RecoveryActionType.ARCHIVE,
            RecoveryActionType.DEACTIVATE,
            RecoveryActionType.ANONYMIZE,
            RecoveryActionType.HARD_DELETE
    );

    private static final String META_CHANGE_TICKET = "changeTicket";

    private final RecoverableEntityRegistry recoverableEntityRegistry;
    private final RecoveryRecordRepository recoveryRecordRepository;
    private final RecoveryVersionRepository recoveryVersionRepository;
    private final RecoveryAuditLogRepository recoveryAuditLogRepository;
    private final RecoveryActionJobRepository recoveryActionJobRepository;
    private final TenantScopeResolver tenantScopeResolver;
    private final RecoveryProperties recoveryProperties;
    private final RecoveryMetricsRecorder recoveryMetricsRecorder;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RecoveryEventStreamService recoveryEventStreamService;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<RecoveryBulkJobProcessor> recoveryBulkJobProcessorProvider;

    /**
     * Creates a new recovery governance service implementation.
     *
     * @param recoverableEntityRegistry The recoverable entity registry.
     * @param recoveryRecordRepository The recovery record repository.
     * @param recoveryVersionRepository The recovery version repository.
     * @param recoveryAuditLogRepository The recovery audit-log repository.
     * @param recoveryActionJobRepository The recovery action-job repository.
     * @param tenantScopeResolver The tenant scope resolver.
     * @param recoveryProperties The recovery configuration properties.
     * @param recoveryMetricsRecorder The recovery metrics recorder.
     * @param applicationEventPublisher The application event publisher.
     * @param objectMapper The object mapper.
     * @param recoveryBulkJobProcessorProvider The asynchronous bulk-job processor provider.
     */
    public RecoveryGovernanceServiceImpl(
            RecoverableEntityRegistry recoverableEntityRegistry,
            RecoveryRecordRepository recoveryRecordRepository,
            RecoveryVersionRepository recoveryVersionRepository,
            RecoveryAuditLogRepository recoveryAuditLogRepository,
            RecoveryActionJobRepository recoveryActionJobRepository,
            TenantScopeResolver tenantScopeResolver,
            RecoveryProperties recoveryProperties,
            RecoveryMetricsRecorder recoveryMetricsRecorder,
            ApplicationEventPublisher applicationEventPublisher,
            RecoveryEventStreamService recoveryEventStreamService,
            ObjectMapper objectMapper,
            ObjectProvider<RecoveryBulkJobProcessor> recoveryBulkJobProcessorProvider
    ) {
        this.recoverableEntityRegistry = recoverableEntityRegistry;
        this.recoveryRecordRepository = recoveryRecordRepository;
        this.recoveryVersionRepository = recoveryVersionRepository;
        this.recoveryAuditLogRepository = recoveryAuditLogRepository;
        this.recoveryActionJobRepository = recoveryActionJobRepository;
        this.tenantScopeResolver = tenantScopeResolver;
        this.recoveryProperties = recoveryProperties;
        this.recoveryMetricsRecorder = recoveryMetricsRecorder;
        this.applicationEventPublisher = applicationEventPublisher;
        this.recoveryEventStreamService = recoveryEventStreamService;
        this.objectMapper = objectMapper;
        this.recoveryBulkJobProcessorProvider = recoveryBulkJobProcessorProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<RecoveryRecordDto> listRecords(String entityType, RecoveryLifecycleState lifecycleState, String query, Pageable pageable) {
        String tenantKey = tenantScopeResolver.resolveCurrentTenant();
        return recoveryRecordRepository.findAll(buildRecordSpec(tenantKey, entityType, lifecycleState, query), pageable)
                .map(this::toRecordDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<RecoveryVersionDto> listVersions(String entityType, String entityId) {
        String tenantKey = tenantScopeResolver.resolveCurrentTenant();
        String normalizedEntityType = recoverableEntityRegistry.normalize(entityType);
        return recoveryVersionRepository.findAllByTenantKeyAndEntityTypeAndEntityIdOrderByVersionNumberDesc(tenantKey, normalizedEntityType, entityId)
                .stream()
                .map(this::toVersionDto)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<RecoveryAuditLogDto> listAuditLogs(
            String entityType,
            RecoveryActionType actionType,
            String actionStatus,
            String query,
            Boolean errorsOnly,
            Pageable pageable
    ) {
        String tenantKey = tenantScopeResolver.resolveCurrentTenant();
        return recoveryAuditLogRepository.findAll(buildAuditLogSpec(tenantKey, entityType, actionType, actionStatus, query, errorsOnly), pageable)
                .map(this::toAuditLogDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<RecoveryActionJobDto> listJobs(
            String entityType,
            RecoveryJobStatus status,
            String query,
            Boolean errorsOnly,
            Pageable pageable
    ) {
        String tenantKey = tenantScopeResolver.resolveCurrentTenant();
        return recoveryActionJobRepository.findAll(buildJobSpec(tenantKey, entityType, status, query, errorsOnly), pageable)
                .map(this::toJobDto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public RecoveryActionResultDto applyAction(RecoveryActionRequest request, String actor) {
        if (recoveryProperties.getApprovalRequiredActions().contains(request.actionType())) {
            throw new BadRequestException(
                    "RECOVERY_APPROVAL_REQUIRED",
                    "Approval is required for this action. Submit an approval request first."
            );
        }
        return applyActionInternal(request, actor, tenantScopeResolver.resolveCurrentTenant());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public RecoveryActionJobDto submitBulkAction(RecoveryBulkActionRequest request, String actor) {
        String tenantKey = tenantScopeResolver.resolveCurrentTenant();
        return submitBulkActionInternal(request, actor, tenantKey, false);
    }

    @Override
    @Transactional
    public RecoveryActionJobDto requestJobCancel(UUID jobId, String actor) {
        String tenantKey = tenantScopeResolver.resolveCurrentTenant();
        RecoveryActionJob job = recoveryActionJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("RECOVERY_JOB_NOT_FOUND", "Recovery action job not found."));
        if (!tenantKey.equals(job.getTenantKey())) {
            throw new NotFoundException("RECOVERY_JOB_NOT_FOUND", "Recovery action job not found.");
        }

        RecoveryJobStatus status = job.getStatus();
        if (status == RecoveryJobStatus.CANCELLED || status == RecoveryJobStatus.CANCEL_REQUESTED) {
            return toJobDto(job);
        }

        if (status == RecoveryJobStatus.QUEUED || status == RecoveryJobStatus.RUNNING || status == RecoveryJobStatus.VALIDATING) {
            job.setStatus(RecoveryJobStatus.CANCEL_REQUESTED);
            job.setErrorSummary(truncate("Cancel requested by " + (actor == null ? "operator" : actor) + ".", 1000));
            RecoveryActionJob saved = recoveryActionJobRepository.save(job);
            recoveryEventStreamService.publish("recovery.job", toJobDto(saved));
            return toJobDto(saved);
        }

        throw new BadRequestException("RECOVERY_JOB_CANCEL_FORBIDDEN", "Only queued or running jobs can be cancelled.");
    }

    @Override
    @Transactional
    public RecoveryActionJobDto retryJob(UUID jobId, boolean failedOnly, String actor) {
        String tenantKey = tenantScopeResolver.resolveCurrentTenant();
        RecoveryActionJob job = recoveryActionJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("RECOVERY_JOB_NOT_FOUND", "Recovery action job not found."));
        if (!tenantKey.equals(job.getTenantKey())) {
            throw new NotFoundException("RECOVERY_JOB_NOT_FOUND", "Recovery action job not found.");
        }
        if (job.getRequestPayloadJson() == null || job.getRequestPayloadJson().isBlank()) {
            throw new BadRequestException("RECOVERY_JOB_PAYLOAD_MISSING", "This job cannot be retried because the original request payload was not stored.");
        }

        RecoveryBulkActionRequest originalRequest;
        try {
            originalRequest = objectMapper.readValue(job.getRequestPayloadJson(), RecoveryBulkActionRequest.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse stored job request payload.", exception);
        }

        List<String> entityIds = failedOnly ? resolveFailedEntityIds(job) : sanitizeEntityIds(originalRequest.entityIds());
        if (entityIds.isEmpty()) {
            throw new BadRequestException("RECOVERY_JOB_RETRY_EMPTY", "No matching items are available to retry for this job.");
        }

        Map<String, Object> metadata = new HashMap<>();
        if (originalRequest.metadata() != null) {
            metadata.putAll(originalRequest.metadata());
        }
        metadata.put("retryOfJobId", String.valueOf(jobId));

        RecoveryBulkActionRequest retryRequest = new RecoveryBulkActionRequest(
                originalRequest.entityType(),
                originalRequest.actionType(),
                entityIds,
                originalRequest.reason(),
                originalRequest.dryRun(),
                originalRequest.restoreTo(),
                originalRequest.legalHoldUntil(),
                originalRequest.retentionDays(),
                metadata
        );

        return submitBulkActionInternal(retryRequest, actor, tenantKey, true);
    }

    @Override
    @Transactional(readOnly = true)
    public String failureReportCsv(UUID jobId) {
        String tenantKey = tenantScopeResolver.resolveCurrentTenant();
        RecoveryActionJob job = recoveryActionJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("RECOVERY_JOB_NOT_FOUND", "Recovery action job not found."));
        if (!tenantKey.equals(job.getTenantKey())) {
            throw new NotFoundException("RECOVERY_JOB_NOT_FOUND", "Recovery action job not found.");
        }
        if (job.getResultSummaryJson() == null || job.getResultSummaryJson().isBlank()) {
            return "entityId,status,state,error\n";
        }

        StringBuilder csv = new StringBuilder();
        csv.append("entityId,status,state,error\n");

        try {
            JsonNode root = objectMapper.readTree(job.getResultSummaryJson());
            if (root == null || !root.isArray()) {
                return csv.toString();
            }
            for (JsonNode entry : root) {
                String status = entry.path("status").asText("");
                if (!"FAILED".equalsIgnoreCase(status)) {
                    continue;
                }
                csv.append(csvEscape(entry.path("entityId").asText("")));
                csv.append(',');
                csv.append(csvEscape(status));
                csv.append(',');
                csv.append(csvEscape(entry.path("state").asText("")));
                csv.append(',');
                csv.append(csvEscape(entry.path("error").asText("")));
                csv.append('\n');
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse recovery job result summary.", exception);
        }

        return csv.toString();
    }

    private List<String> resolveFailedEntityIds(RecoveryActionJob job) {
        if (job.getResultSummaryJson() == null || job.getResultSummaryJson().isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(job.getResultSummaryJson());
            if (root == null || !root.isArray()) {
                return List.of();
            }
            List<String> failed = new ArrayList<>();
            for (JsonNode entry : root) {
                if (!"FAILED".equalsIgnoreCase(entry.path("status").asText())) {
                    continue;
                }
                String entityId = entry.path("entityId").asText(null);
                if (entityId != null && !entityId.isBlank()) {
                    failed.add(entityId.trim());
                }
            }
            return failed.stream().distinct().toList();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse recovery job result summary.", exception);
        }
    }

    private static String csvEscape(String value) {
        String text = value == null ? "" : value;
        if (!text.contains(",") && !text.contains("\"") && !text.contains("\n") && !text.contains("\r")) {
            return text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, max);
    }

    /**
     * Internal bulk-action submission hook used by approvals and retry flows.
     *
     * @param request The bulk action payload.
     * @param actor The submitting actor.
     * @param tenantKey The resolved tenant scope.
     * @param bypassApproval Whether to bypass approval requirements (used after approval or for retries).
     * @return The queued job DTO.
     */
    @Transactional
    public RecoveryActionJobDto submitBulkActionInternal(
            RecoveryBulkActionRequest request,
            String actor,
            String tenantKey,
            boolean bypassApproval
    ) {
        if (!bypassApproval && !request.dryRun() && recoveryProperties.getApprovalRequiredActions().contains(request.actionType())) {
            throw new BadRequestException(
                    "RECOVERY_APPROVAL_REQUIRED",
                    "Approval is required for this bulk action. Submit an approval request first."
            );
        }

        String normalizedEntityType = recoverableEntityRegistry.normalize(request.entityType());
        recoverableEntityRegistry.getRequiredAdapter(normalizedEntityType);

        List<String> entityIds = sanitizeEntityIds(request.entityIds());
        if (entityIds.isEmpty()) {
            throw new BadRequestException("RECOVERY_BULK_IDS_REQUIRED", "Bulk requests require at least one entity id.");
        }

        if (!request.dryRun()) {
            String changeTicket = extractChangeTicket(request.metadata());
            if (changeTicket == null) {
                throw new BadRequestException("RECOVERY_CHANGE_TICKET_REQUIRED", "changeTicket is required for bulk non-dry-run actions.");
            }
            if (DESTRUCTIVE_ACTIONS.contains(request.actionType())) {
                String reason = request.reason() == null ? "" : request.reason().trim();
                if (reason.length() < recoveryProperties.getMinReasonLength()) {
                    throw new BadRequestException(
                            "RECOVERY_REASON_REQUIRED",
                            "Bulk destructive actions require a business reason (at least " + recoveryProperties.getMinReasonLength() + " characters)."
                    );
                }
            }
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        if (request.metadata() != null) {
            metadata.putAll(request.metadata());
        }
        String correlationId = MDC.get("correlationId");
        if (correlationId != null && !correlationId.isBlank()) {
            metadata.putIfAbsent("correlationId", correlationId.trim());
        }

        RecoveryBulkActionRequest sanitizedRequest = new RecoveryBulkActionRequest(
                request.entityType(),
                request.actionType(),
                entityIds,
                request.reason(),
                request.dryRun(),
                request.restoreTo(),
                request.legalHoldUntil(),
                request.retentionDays(),
                metadata.isEmpty() ? null : metadata
        );

        RecoveryActionJob job = new RecoveryActionJob();
        job.setTenantKey(tenantKey);
        job.setEntityType(normalizedEntityType);
        job.setActionType(request.actionType());
        job.setStatus(RecoveryJobStatus.QUEUED);
        job.setRequestedBy(actor);
        job.setDryRun(request.dryRun());
        job.setTotalItems(entityIds.size());
        job.setValidationSummaryJson(toJson(Map.of(
                "duplicatesRemoved", request.entityIds().size() - entityIds.size(),
                "requestedItems", request.entityIds().size()
        )));
        job.setRequestPayloadJson(toJson(sanitizedRequest));

        RecoveryActionJob saved = recoveryActionJobRepository.save(job);
        recoveryEventStreamService.publish("recovery.job", toJobDto(saved));
        RecoveryBulkJobProcessor recoveryBulkJobProcessor = recoveryBulkJobProcessorProvider.getObject();
        recoveryBulkJobProcessor.processJob(saved.getId(), sanitizedRequest, actor, tenantKey);
        return toJobDto(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void captureVersion(String entityType, String entityId, RecoveryActionType actionType, String actor, String reason, Map<String, Object> metadata) {
        String tenantKey = tenantScopeResolver.resolveCurrentTenant();
        String normalizedEntityType = recoverableEntityRegistry.normalize(entityType);
        RecoverableEntityAdapter adapter = recoverableEntityRegistry.getRequiredAdapter(normalizedEntityType);
        RecoverableEntityHandle handle = adapter.findHandle(entityId)
                .orElseThrow(() -> new NotFoundException("RECOVERY_ENTITY_NOT_FOUND", "Governed entity not found."));
        RecoveryRecord record = getOrCreateRecord(tenantKey, normalizedEntityType, entityId, handle.getDisplayName());
        record.setDisplayName(handle.getDisplayName());
        RecoveryVersion version = saveVersion(
                record,
                actionType,
                resolveLifecycleState(record, handle),
                actor,
                reason,
                false,
                false,
                null,
                handle.toSnapshot(),
                metadata
        );
        record.setLastVersionId(version.getId());
        recoveryRecordRepository.save(record);
        writeAuditLog(tenantKey, normalizedEntityType, entityId, actionType, "SUCCESS", actor, "Version captured.", metadata);
        publishDomainEvent(tenantKey, normalizedEntityType, entityId, actionType, record.getLifecycleState(), actor, true);
        recoveryMetricsRecorder.recordAction(normalizedEntityType, actionType, "success");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, RecoveryLifecycleState> resolveLifecycleStates(String entityType, Collection<String> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return Map.of();
        }
        String tenantKey = tenantScopeResolver.resolveCurrentTenant();
        String normalizedEntityType = recoverableEntityRegistry.normalize(entityType);
        return recoveryRecordRepository.findAllByTenantKeyAndEntityTypeAndEntityIdIn(tenantKey, normalizedEntityType, entityIds)
                .stream()
                .collect(Collectors.toMap(RecoveryRecord::getEntityId, RecoveryRecord::getLifecycleState));
    }

    /**
     * Processes an asynchronously queued bulk-action job.
     *
     * @param jobId The job identifier.
     * @param request The bulk action request.
     * @param actor The authenticated actor name.
     * @param tenantKey The tenant scope.
     */
    @Transactional
    public void processQueuedJob(UUID jobId, RecoveryBulkActionRequest request, String actor, String tenantKey) {
        RecoveryActionJob job = recoveryActionJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("RECOVERY_JOB_NOT_FOUND", "Recovery action job not found."));
        job.setStatus(RecoveryJobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        recoveryActionJobRepository.save(job);

        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        for (String entityId : sanitizeEntityIds(request.entityIds())) {
            try {
                if (request.dryRun()) {
                    validateActionRequest(
                            new RecoveryActionRequest(
                                    request.entityType(),
                                    entityId,
                                    request.actionType(),
                                    request.reason(),
                                    request.restoreTo(),
                                    request.legalHoldUntil(),
                                    request.retentionDays(),
                                    request.metadata()
                            ),
                            tenantKey,
                            true
                    );
                    results.add(itemResult(entityId, "VALIDATED", null, null));
                    successCount++;
                } else {
                    RecoveryActionResultDto result = applyActionInternal(
                            new RecoveryActionRequest(
                                    request.entityType(),
                                    entityId,
                                    request.actionType(),
                                    request.reason(),
                                    request.restoreTo(),
                                    request.legalHoldUntil(),
                                    request.retentionDays(),
                                    request.metadata()
                            ),
                            actor,
                            tenantKey
                    );
                    results.add(itemResult(entityId, "SUCCESS", result.lifecycleState().name(), null));
                    successCount++;
                }
            } catch (RuntimeException exception) {
                failedCount++;
                results.add(itemResult(entityId, "FAILED", null, exception.getMessage()));
            }
            job.setProcessedItems(results.size());
            job.setSuccessItems(successCount);
            job.setFailedItems(failedCount);
            recoveryActionJobRepository.save(job);
        }

        job.setCompletedAt(Instant.now());
        job.setResultSummaryJson(toJson(results));
        if (failedCount == 0) {
            job.setStatus(RecoveryJobStatus.COMPLETED);
        } else if (successCount == 0) {
            job.setStatus(RecoveryJobStatus.FAILED);
        } else {
            job.setStatus(RecoveryJobStatus.PARTIAL_SUCCESS);
        }
        recoveryActionJobRepository.save(job);
    }

    /**
     * Executes a governed action for an explicit tenant scope.
     *
     * @param request The requested action payload.
     * @param actor The authenticated actor name.
     * @param tenantKey The tenant scope.
     * @return The action result.
     */
    @Transactional
    public RecoveryActionResultDto applyActionInternal(RecoveryActionRequest request, String actor, String tenantKey) {
        validateActionRequest(request, tenantKey, false);

        String entityType = recoverableEntityRegistry.normalize(request.entityType());
        String entityId = request.entityId().trim();
        RecoveryActionType actionType = request.actionType();
        RecoverableEntityAdapter adapter = recoverableEntityRegistry.getRequiredAdapter(entityType);
        RecoveryRecord record = recoveryRecordRepository.findByTenantKeyAndEntityTypeAndEntityId(tenantKey, entityType, entityId)
                .orElseGet(() -> {
                    RecoverableEntityHandle handle = adapter.findHandle(entityId)
                            .orElseThrow(() -> new NotFoundException("RECOVERY_ENTITY_NOT_FOUND", "Governed entity not found."));
                    return getOrCreateRecord(tenantKey, entityType, entityId, handle.getDisplayName());
                });

        try {
            RecoveryActionResultDto result = switch (actionType) {
                case ACTIVATE -> applyStatefulMutation(record, adapter, entityId, actionType, RecoveryLifecycleState.ACTIVE, actor, request);
                case DEACTIVATE -> applyStatefulMutation(record, adapter, entityId, actionType, RecoveryLifecycleState.INACTIVE, actor, request);
                case ARCHIVE -> applyStatefulMutation(record, adapter, entityId, actionType, RecoveryLifecycleState.ARCHIVED, actor, request);
                case TRASH -> applyStatefulMutation(record, adapter, entityId, actionType, RecoveryLifecycleState.TRASHED, actor, request);
                case RESTORE, UNDO_TRASH -> restoreEntity(record, adapter, entityId, actionType, actor, request, null);
                case RESTORE_POINT_IN_TIME -> restoreEntity(record, adapter, entityId, actionType, actor, request, request.restoreTo());
                case HARD_DELETE -> hardDelete(record, adapter, entityId, actor, request);
                case APPLY_LEGAL_HOLD -> applyLegalHold(record, actor, request);
                case RELEASE_LEGAL_HOLD -> releaseLegalHold(record, actor, request);
                case ANONYMIZE -> anonymizeRecord(record, adapter, entityId, actor, request);
                default -> throw new BadRequestException("RECOVERY_ACTION_UNSUPPORTED", "Unsupported governed action: " + actionType);
            };
            publishDomainEvent(tenantKey, entityType, entityId, actionType, result.lifecycleState(), actor, true);
            recoveryMetricsRecorder.recordAction(entityType, actionType, "success");
            return result;
        } catch (RuntimeException exception) {
            writeAuditLog(tenantKey, entityType, entityId, actionType, "FAILED", actor, exception.getMessage(), request.metadata());
            publishDomainEvent(tenantKey, entityType, entityId, actionType, record.getLifecycleState(), actor, false);
            recoveryMetricsRecorder.recordAction(entityType, actionType, "failed");
            throw exception;
        }
    }

    /**
     * Validates a governed action request for a specific tenant without applying mutations.
     *
     * @param request The requested action payload.
     * @param tenantKey The tenant scope.
     * @param dryRun Whether the validation is performed as part of a dry-run job.
     */
    @Transactional(readOnly = true)
    public void validateActionInternal(RecoveryActionRequest request, String tenantKey, boolean dryRun) {
        validateActionRequest(request, tenantKey, dryRun);
    }

    /**
     * Applies a lifecycle-state mutation to an existing recoverable entity.
     *
     * @param record The governed recovery record.
     * @param adapter The recoverable entity adapter.
     * @param entityId The business entity identifier.
     * @param actionType The requested action.
     * @param targetState The target lifecycle state.
     * @param actor The authenticated actor name.
     * @param request The requested action payload.
     * @return The action result.
     */
    private RecoveryActionResultDto applyStatefulMutation(
            RecoveryRecord record,
            RecoverableEntityAdapter adapter,
            String entityId,
            RecoveryActionType actionType,
            RecoveryLifecycleState targetState,
            String actor,
            RecoveryActionRequest request
    ) {
        ensureNotUnderActiveLegalHold(record);
        RecoverableEntityHandle handle = adapter.findHandle(entityId)
                .orElseThrow(() -> new NotFoundException("RECOVERY_ENTITY_NOT_FOUND", "Governed entity not found."));
        handle.applyLifecycleState(targetState);
        handle.persist();
        record.setDisplayName(handle.getDisplayName());
        applyRecordState(record, targetState, actor, request.reason(), request.retentionDays(), request.legalHoldUntil());
        RecoveryVersion version = saveVersion(
                record,
                actionType,
                targetState,
                actor,
                request.reason(),
                false,
                false,
                null,
                handle.toSnapshot(),
                request.metadata()
        );
        record.setLastVersionId(version.getId());
        recoveryRecordRepository.save(record);
        writeAuditLog(record.getTenantKey(), record.getEntityType(), entityId, actionType, "SUCCESS", actor, "Lifecycle state updated.", request.metadata());
        return new RecoveryActionResultDto(record.getEntityType(), entityId, actionType, targetState, "Lifecycle state updated.", toRecordDto(record));
    }

    /**
     * Restores an entity from current or historical snapshots.
     *
     * @param record The governed recovery record.
     * @param adapter The recoverable entity adapter.
     * @param entityId The business entity identifier.
     * @param actionType The requested action.
     * @param actor The authenticated actor name.
     * @param request The requested action payload.
     * @param restoreTo The optional point-in-time restore timestamp.
     * @return The action result.
     */
    private RecoveryActionResultDto restoreEntity(
            RecoveryRecord record,
            RecoverableEntityAdapter adapter,
            String entityId,
            RecoveryActionType actionType,
            String actor,
            RecoveryActionRequest request,
            Instant restoreTo
    ) {
        RecoveryLifecycleState targetState = RecoveryLifecycleState.ACTIVE;
        RecoverableEntityHandle handle = adapter.findHandle(entityId).orElseGet(() -> {
            RecoveryVersion sourceVersion = resolveRestoreVersion(record, restoreTo);
            JsonNode snapshot = parseJsonNode(sourceVersion.getSnapshotJson());
            return adapter.restoreHandle(entityId, snapshot);
        });
        if (restoreTo != null || TERMINAL_STATES.contains(record.getLifecycleState())) {
            RecoveryVersion sourceVersion = resolveRestoreVersion(record, restoreTo);
            handle.restoreFromSnapshot(parseJsonNode(sourceVersion.getSnapshotJson()), targetState);
        } else {
            handle.applyLifecycleState(targetState);
        }
        handle.persist();
        record.setDisplayName(handle.getDisplayName());
        applyRecordState(record, targetState, actor, request.reason(), request.retentionDays(), request.legalHoldUntil());
        RecoveryVersion version = saveVersion(
                record,
                actionType,
                targetState,
                actor,
                request.reason(),
                false,
                false,
                restoreTo,
                handle.toSnapshot(),
                request.metadata()
        );
        record.setLastVersionId(version.getId());
        recoveryRecordRepository.save(record);
        writeAuditLog(record.getTenantKey(), record.getEntityType(), entityId, actionType, "SUCCESS", actor, "Entity restored.", request.metadata());
        return new RecoveryActionResultDto(record.getEntityType(), entityId, actionType, targetState, "Entity restored.", toRecordDto(record));
    }

    /**
     * Permanently deletes an entity after a full backup snapshot has been captured.
     *
     * @param record The governed recovery record.
     * @param adapter The recoverable entity adapter.
     * @param entityId The business entity identifier.
     * @param actor The authenticated actor name.
     * @param request The requested action payload.
     * @return The action result.
     */
    private RecoveryActionResultDto hardDelete(
            RecoveryRecord record,
            RecoverableEntityAdapter adapter,
            String entityId,
            String actor,
            RecoveryActionRequest request
    ) {
        ensureNotUnderActiveLegalHold(record);
        RecoverableEntityHandle handle = adapter.findHandle(entityId)
                .orElseThrow(() -> new NotFoundException("RECOVERY_ENTITY_NOT_FOUND", "Governed entity not found."));
        RecoveryVersion backupVersion = saveVersion(
                record,
                RecoveryActionType.HARD_DELETE,
                RecoveryLifecycleState.PURGED,
                actor,
                request.reason(),
                true,
                false,
                null,
                handle.toSnapshot(),
                request.metadata()
        );
        record.setBackupVerified(true);
        record.setLastVersionId(backupVersion.getId());
        recoveryRecordRepository.save(record);
        adapter.hardDelete(entityId);
        applyRecordState(record, RecoveryLifecycleState.PURGED, actor, request.reason(), request.retentionDays(), request.legalHoldUntil());
        recoveryRecordRepository.save(record);
        writeAuditLog(record.getTenantKey(), record.getEntityType(), entityId, RecoveryActionType.HARD_DELETE, "SUCCESS", actor, "Entity permanently deleted after backup.", request.metadata());
        return new RecoveryActionResultDto(record.getEntityType(), entityId, RecoveryActionType.HARD_DELETE, RecoveryLifecycleState.PURGED, "Entity permanently deleted.", toRecordDto(record));
    }

    /**
     * Applies a legal hold to a governed recovery record.
     *
     * @param record The governed recovery record.
     * @param actor The authenticated actor name.
     * @param request The requested action payload.
     * @return The action result.
     */
    private RecoveryActionResultDto applyLegalHold(RecoveryRecord record, String actor, RecoveryActionRequest request) {
        if (request.legalHoldUntil() == null) {
            throw new BadRequestException("RECOVERY_LEGAL_HOLD_REQUIRED", "legalHoldUntil is required when applying a legal hold.");
        }
        record.setLegalHoldUntil(request.legalHoldUntil());
        record.setLastActionBy(actor);
        record.setLastReason(request.reason());
        recoveryRecordRepository.save(record);
        writeAuditLog(record.getTenantKey(), record.getEntityType(), record.getEntityId(), RecoveryActionType.APPLY_LEGAL_HOLD, "SUCCESS", actor, "Legal hold applied.", request.metadata());
        return new RecoveryActionResultDto(record.getEntityType(), record.getEntityId(), RecoveryActionType.APPLY_LEGAL_HOLD, record.getLifecycleState(), "Legal hold applied.", toRecordDto(record));
    }

    /**
     * Releases a legal hold from a governed recovery record.
     *
     * @param record The governed recovery record.
     * @param actor The authenticated actor name.
     * @param request The requested action payload.
     * @return The action result.
     */
    private RecoveryActionResultDto releaseLegalHold(RecoveryRecord record, String actor, RecoveryActionRequest request) {
        record.setLegalHoldUntil(null);
        record.setLastActionBy(actor);
        record.setLastReason(request.reason());
        recoveryRecordRepository.save(record);
        writeAuditLog(record.getTenantKey(), record.getEntityType(), record.getEntityId(), RecoveryActionType.RELEASE_LEGAL_HOLD, "SUCCESS", actor, "Legal hold released.", request.metadata());
        return new RecoveryActionResultDto(record.getEntityType(), record.getEntityId(), RecoveryActionType.RELEASE_LEGAL_HOLD, record.getLifecycleState(), "Legal hold released.", toRecordDto(record));
    }

    /**
     * Anonymizes governed recovery history and removes the live entity when present.
     *
     * @param record The governed recovery record.
     * @param adapter The recoverable entity adapter.
     * @param entityId The business entity identifier.
     * @param actor The authenticated actor name.
     * @param request The requested action payload.
     * @return The action result.
     */
    private RecoveryActionResultDto anonymizeRecord(
            RecoveryRecord record,
            RecoverableEntityAdapter adapter,
            String entityId,
            String actor,
            RecoveryActionRequest request
    ) {
        JsonNode sourceSnapshot = adapter.findHandle(entityId)
                .map(RecoverableEntityHandle::toSnapshot)
                .map(this::toJsonNode)
                .orElseGet(() -> {
                    List<RecoveryVersion> versions = recoveryVersionRepository.findAllByTenantKeyAndEntityTypeAndEntityIdOrderByVersionNumberDesc(
                            record.getTenantKey(),
                            record.getEntityType(),
                            entityId
                    );
                    if (versions.isEmpty()) {
                        throw new NotFoundException("RECOVERY_VERSION_NOT_FOUND", "No recovery snapshot is available for anonymization.");
                    }
                    return parseJsonNode(versions.getFirst().getSnapshotJson());
                });
        JsonNode anonymizedSnapshot = anonymizeNode(sourceSnapshot);
        saveVersion(
                record,
                RecoveryActionType.ANONYMIZE,
                RecoveryLifecycleState.ANONYMIZED,
                actor,
                request.reason(),
                true,
                true,
                null,
                anonymizedSnapshot,
                request.metadata()
        );
        adapter.findHandle(entityId).ifPresent(handle -> adapter.hardDelete(handle.getEntityId()));
        record.setAnonymized(true);
        applyRecordState(record, RecoveryLifecycleState.ANONYMIZED, actor, request.reason(), request.retentionDays(), request.legalHoldUntil());
        recoveryRecordRepository.save(record);
        writeAuditLog(record.getTenantKey(), record.getEntityType(), entityId, RecoveryActionType.ANONYMIZE, "SUCCESS", actor, "Recovery history anonymized.", request.metadata());
        return new RecoveryActionResultDto(record.getEntityType(), entityId, RecoveryActionType.ANONYMIZE, RecoveryLifecycleState.ANONYMIZED, "Recovery history anonymized.", toRecordDto(record));
    }

    /**
     * Validates a recovery action request before execution.
     *
     * @param request The requested action payload.
     * @param tenantKey The tenant scope.
     */
    private void validateActionRequest(RecoveryActionRequest request, String tenantKey, boolean dryRun) {
        String entityType = recoverableEntityRegistry.normalize(request.entityType());
        if (recoveryRecordRepository.existsByEntityTypeAndEntityIdAndTenantKeyNot(entityType, request.entityId().trim(), tenantKey)) {
            throw new ForbiddenException("RECOVERY_TENANT_CONFLICT", "The entity is already governed by a different tenant.");
        }
        if ((request.actionType() == RecoveryActionType.RESTORE_POINT_IN_TIME) && request.restoreTo() == null) {
            throw new BadRequestException("RECOVERY_RESTORE_TIME_REQUIRED", "restoreTo is required for point-in-time restore.");
        }
        enforceReasonAndTicket(request, dryRun);
        recoverableEntityRegistry.getRequiredAdapter(entityType);
    }

    private void enforceReasonAndTicket(RecoveryActionRequest request, boolean dryRun) {
        if (dryRun) {
            return;
        }

        RecoveryActionType actionType = request.actionType();
        if (DESTRUCTIVE_ACTIONS.contains(actionType)) {
            String reason = request.reason() == null ? "" : request.reason().trim();
            if (reason.length() < recoveryProperties.getMinReasonLength()) {
                throw new BadRequestException(
                        "RECOVERY_REASON_REQUIRED",
                        "Provide a business reason (at least " + recoveryProperties.getMinReasonLength() + " characters) before applying destructive actions."
                );
            }
        }

        if (recoveryProperties.getApprovalRequiredActions().contains(actionType)) {
            String changeTicket = extractChangeTicket(request.metadata());
            if (changeTicket == null) {
                throw new BadRequestException(
                        "RECOVERY_CHANGE_TICKET_REQUIRED",
                        "changeTicket is required for high-impact actions."
                );
            }
        }
    }

    /**
     * Resolves the most appropriate restore source version.
     *
     * @param record The governed recovery record.
     * @param restoreTo The optional point-in-time restore timestamp.
     * @return The restore source version.
     */
    private RecoveryVersion resolveRestoreVersion(RecoveryRecord record, Instant restoreTo) {
        if (restoreTo != null) {
            return recoveryVersionRepository
                    .findFirstByTenantKeyAndEntityTypeAndEntityIdAndCapturedAtLessThanEqualOrderByCapturedAtDesc(
                            record.getTenantKey(),
                            record.getEntityType(),
                            record.getEntityId(),
                            restoreTo
                    )
                    .orElseThrow(() -> new NotFoundException("RECOVERY_VERSION_NOT_FOUND", "No snapshot exists at the requested restore point."));
        }
        return recoveryVersionRepository.findAllByTenantKeyAndEntityTypeAndEntityIdOrderByVersionNumberDesc(
                        record.getTenantKey(),
                        record.getEntityType(),
                        record.getEntityId()
                ).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("RECOVERY_VERSION_NOT_FOUND", "No snapshot exists for the requested restore operation."));
    }

    /**
     * Returns an existing recovery record or creates a new governed record shell.
     *
     * @param tenantKey The tenant scope.
     * @param entityType The business entity type.
     * @param entityId The business entity identifier.
     * @param displayName The operator-facing display name.
     * @return The governed recovery record.
     */
    private RecoveryRecord getOrCreateRecord(String tenantKey, String entityType, String entityId, String displayName) {
        return recoveryRecordRepository.findByTenantKeyAndEntityTypeAndEntityId(tenantKey, entityType, entityId)
                .orElseGet(() -> {
                    RecoveryRecord record = new RecoveryRecord();
                    record.setTenantKey(tenantKey);
                    record.setEntityType(entityType);
                    record.setEntityId(entityId);
                    record.setDisplayName(displayName);
                    return record;
                });
    }

    /**
     * Saves an immutable version snapshot for a governed entity.
     *
     * @param record The governed recovery record.
     * @param actionType The action associated with the snapshot.
     * @param lifecycleState The lifecycle state after the action.
     * @param actor The authenticated actor name.
     * @param reason The operator-facing reason.
     * @param backupSnapshot Whether the snapshot is a hard-delete backup.
     * @param anonymized Whether the snapshot payload is anonymized.
     * @param restoredFromAt The restore source timestamp, when applicable.
     * @param snapshot The serializable snapshot payload.
     * @param metadata Optional structured metadata.
     * @return The saved recovery version.
     */
    private RecoveryVersion saveVersion(
            RecoveryRecord record,
            RecoveryActionType actionType,
            RecoveryLifecycleState lifecycleState,
            String actor,
            String reason,
            boolean backupSnapshot,
            boolean anonymized,
            Instant restoredFromAt,
            Object snapshot,
            Map<String, Object> metadata
    ) {
        RecoveryVersion version = new RecoveryVersion();
        version.setRecoveryRecord(record.getId() == null ? null : recoveryRecordRepository.save(record));
        version.setTenantKey(record.getTenantKey());
        version.setEntityType(record.getEntityType());
        version.setEntityId(record.getEntityId());
        version.setVersionNumber(Optional.ofNullable(record.getCurrentVersionNumber()).orElse(0) + 1);
        version.setActionType(actionType);
        version.setLifecycleStateAfter(lifecycleState);
        version.setCapturedAt(Instant.now());
        version.setActor(actor);
        version.setReason(reason);
        version.setBackupSnapshot(backupSnapshot);
        version.setAnonymized(anonymized);
        version.setRestoredFromAt(restoredFromAt);
        version.setSnapshotJson(toJson(snapshot));
        version.setMetadataJson(toJson(metadata));
        RecoveryVersion saved = recoveryVersionRepository.save(version);
        record.setCurrentVersionNumber(saved.getVersionNumber());
        return saved;
    }

    /**
     * Applies lifecycle metadata to a governed recovery record.
     *
     * @param record The governed recovery record.
     * @param state The new lifecycle state.
     * @param actor The authenticated actor name.
     * @param reason The operator-facing reason.
     * @param retentionDays The retention override, in days.
     * @param legalHoldUntil The legal-hold expiry timestamp.
     */
    private void applyRecordState(
            RecoveryRecord record,
            RecoveryLifecycleState state,
            String actor,
            String reason,
            Integer retentionDays,
            Instant legalHoldUntil
    ) {
        Instant now = Instant.now();
        record.setLifecycleState(state);
        record.setLastActionBy(actor);
        record.setLastReason(reason);
        if (legalHoldUntil != null) {
            record.setLegalHoldUntil(legalHoldUntil);
        }
        switch (state) {
            case ACTIVE -> {
                record.setRestoredAt(now);
                record.setDeletedAt(null);
                record.setArchivedAt(null);
                record.setInactivatedAt(null);
                record.setPurgedAt(null);
            }
            case INACTIVE -> record.setInactivatedAt(now);
            case ARCHIVED -> record.setArchivedAt(now);
            case TRASHED -> {
                record.setDeletedAt(now);
                int retentionWindow = retentionDays == null ? recoveryProperties.getDefaultRetentionDays() : retentionDays;
                record.setRetentionUntil(now.plus(retentionWindow, ChronoUnit.DAYS));
            }
            case PURGED, ANONYMIZED -> record.setPurgedAt(now);
        }
    }

    /**
     * Resolves the effective lifecycle state for version capture.
     *
     * @param record The governed recovery record.
     * @param handle The recoverable entity handle.
     * @return The effective lifecycle state.
     */
    private RecoveryLifecycleState resolveLifecycleState(RecoveryRecord record, RecoverableEntityHandle handle) {
        return Optional.ofNullable(record.getLifecycleState()).orElse(RecoveryLifecycleState.ACTIVE);
    }

    /**
     * Throws when a governed record is under an active legal hold.
     *
     * @param record The governed recovery record.
     */
    private void ensureNotUnderActiveLegalHold(RecoveryRecord record) {
        if (record.getLegalHoldUntil() != null && record.getLegalHoldUntil().isAfter(Instant.now())) {
            throw new ForbiddenException("RECOVERY_LEGAL_HOLD_ACTIVE", "The requested record is under an active legal hold.");
        }
    }

    /**
     * Writes an immutable recovery audit-log entry.
     *
     * @param tenantKey The tenant scope.
     * @param entityType The business entity type.
     * @param entityId The business entity identifier.
     * @param actionType The governed action.
     * @param status The final action status.
     * @param actor The authenticated actor name.
     * @param message The audit message.
     * @param metadata Optional structured metadata.
     */
    private void writeAuditLog(
            String tenantKey,
            String entityType,
            String entityId,
            RecoveryActionType actionType,
            String status,
            String actor,
            String message,
            Map<String, Object> metadata
    ) {
        RecoveryAuditLog auditLog = new RecoveryAuditLog();
        auditLog.setTenantKey(tenantKey);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setActionType(actionType);
        auditLog.setActionStatus(status);
        auditLog.setActor(actor);
        auditLog.setCorrelationId(resolveCorrelationId(metadata));
        auditLog.setMessage(message);
        auditLog.setMetadataJson(toJson(metadata));
        auditLog.setOccurredAt(Instant.now());
        RecoveryAuditLog saved = recoveryAuditLogRepository.save(auditLog);
        recoveryEventStreamService.publish("recovery.audit", toAuditLogDto(saved));
    }

    /**
     * Publishes a recovery domain event.
     *
     * @param tenantKey The tenant scope.
     * @param entityType The business entity type.
     * @param entityId The business entity identifier.
     * @param actionType The governed action.
     * @param lifecycleState The resulting lifecycle state.
     * @param actor The authenticated actor name.
     * @param successful Whether the action completed successfully.
     */
    private void publishDomainEvent(
            String tenantKey,
            String entityType,
            String entityId,
            RecoveryActionType actionType,
            RecoveryLifecycleState lifecycleState,
            String actor,
            boolean successful
    ) {
        applicationEventPublisher.publishEvent(new RecoveryDomainEvent(
                tenantKey,
                entityType,
                entityId,
                actionType,
                lifecycleState,
                actor,
                Instant.now(),
                successful
        ));
    }

    /**
     * Builds a specification for filtered recovery-record queries.
     *
     * @param tenantKey The tenant scope.
     * @param entityType The optional entity-type filter.
     * @param lifecycleState The optional lifecycle-state filter.
     * @param query The optional free-text filter.
     * @return The JPA specification.
     */
    private Specification<RecoveryRecord> buildRecordSpec(String tenantKey, String entityType, RecoveryLifecycleState lifecycleState, String query) {
        return (root, ignored, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenantKey"), tenantKey));
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(builder.equal(root.get("entityType"), recoverableEntityRegistry.normalize(entityType)));
            }
            if (lifecycleState != null) {
                predicates.add(builder.equal(root.get("lifecycleState"), lifecycleState));
            }
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("displayName")), like),
                        builder.like(builder.lower(root.get("entityId")), like)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Builds a specification for filtered recovery audit-log queries.
     *
     * @param tenantKey The tenant scope.
     * @param entityType The optional entity-type filter.
     * @param actionType The optional action-type filter.
     * @param actionStatus The optional action-status filter.
     * @param query The optional free-text filter.
     * @param errorsOnly Whether to include only non-successful events.
     * @return The JPA specification.
     */
    private Specification<RecoveryAuditLog> buildAuditLogSpec(
            String tenantKey,
            String entityType,
            RecoveryActionType actionType,
            String actionStatus,
            String query,
            Boolean errorsOnly
    ) {
        return (root, ignored, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenantKey"), tenantKey));
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(builder.equal(root.get("entityType"), recoverableEntityRegistry.normalize(entityType)));
            }
            if (actionType != null) {
                predicates.add(builder.equal(root.get("actionType"), actionType));
            }
            if (actionStatus != null && !actionStatus.isBlank()) {
                predicates.add(builder.equal(builder.lower(root.get("actionStatus")), actionStatus.trim().toLowerCase(Locale.ROOT)));
            }
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("actionType").as(String.class)), like),
                        builder.like(builder.lower(root.get("actionStatus")), like),
                        builder.like(builder.lower(root.get("entityType")), like),
                        builder.like(builder.lower(root.get("entityId")), like),
                        builder.like(builder.lower(root.get("actor")), like),
                        builder.like(builder.lower(root.get("correlationId")), like),
                        builder.like(builder.lower(root.get("message")), like),
                        builder.like(builder.lower(root.get("metadataJson")), like)
                ));
            }
            if (Boolean.TRUE.equals(errorsOnly)) {
                predicates.add(builder.notEqual(builder.lower(root.get("actionStatus")), "success"));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Builds a specification for filtered recovery job queries.
     *
     * @param tenantKey The tenant scope.
     * @param entityType The optional entity-type filter.
     * @param status The optional job-status filter.
     * @param query The optional free-text filter.
     * @param errorsOnly Whether to include only failed/partial jobs.
     * @return The JPA specification.
     */
    private Specification<RecoveryActionJob> buildJobSpec(
            String tenantKey,
            String entityType,
            RecoveryJobStatus status,
            String query,
            Boolean errorsOnly
    ) {
        return (root, ignored, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("tenantKey"), tenantKey));
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(builder.equal(root.get("entityType"), recoverableEntityRegistry.normalize(entityType)));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("entityType")), like),
                        builder.like(builder.lower(root.get("actionType").as(String.class)), like),
                        builder.like(builder.lower(root.get("status").as(String.class)), like),
                        builder.like(builder.lower(root.get("requestedBy")), like),
                        builder.like(builder.lower(root.get("errorSummary")), like),
                        builder.like(builder.lower(root.get("resultSummaryJson")), like)
                ));
            }
            if (Boolean.TRUE.equals(errorsOnly)) {
                predicates.add(builder.or(
                        root.get("status").in(
                                RecoveryJobStatus.FAILED,
                                RecoveryJobStatus.PARTIAL_SUCCESS,
                                RecoveryJobStatus.CANCELLED,
                                RecoveryJobStatus.CANCEL_REQUESTED
                        ),
                        builder.greaterThan(root.get("failedItems"), 0)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Sanitizes a collection of entity ids for deterministic bulk-job processing.
     *
     * @param entityIds The raw entity ids.
     * @return The sanitized entity ids.
     */
    private List<String> sanitizeEntityIds(Collection<String> entityIds) {
        return entityIds == null ? List.of() : entityIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    /**
     * Serializes a payload to JSON.
     *
     * @param value The payload to serialize.
     * @return The serialized JSON string.
     */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize recovery payload.", exception);
        }
    }

    /**
     * Converts an arbitrary payload to a JSON node.
     *
     * @param value The payload to convert.
     * @return The converted JSON node.
     */
    private JsonNode toJsonNode(Object value) {
        return objectMapper.valueToTree(value);
    }

    /**
     * Parses a JSON payload into a JSON node.
     *
     * @param json The raw JSON payload.
     * @return The parsed JSON node.
     */
    private JsonNode parseJsonNode(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse recovery snapshot payload.", exception);
        }
    }

    /**
     * Resolves a correlation id from structured metadata.
     *
     * @param metadata The structured metadata payload.
     * @return The resolved correlation id.
     */
    private String resolveCorrelationId(Map<String, Object> metadata) {
        if (metadata != null) {
            Object correlationId = metadata.get("correlationId");
            if (correlationId != null) {
                String value = String.valueOf(correlationId).trim();
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        String mdcCorrelation = MDC.get("correlationId");
        if (mdcCorrelation == null || mdcCorrelation.isBlank()) {
            return null;
        }
        return mdcCorrelation.trim();
    }

    private String extractChangeTicket(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        Object value = metadata.get(META_CHANGE_TICKET);
        if (value == null) {
            return null;
        }
        String ticket = String.valueOf(value).trim();
        return ticket.isBlank() ? null : ticket;
    }

    /**
     * Builds a deterministic bulk-job item result payload.
     *
     * @param entityId The business entity identifier.
     * @param status The item status.
     * @param state The resulting lifecycle state, when available.
     * @param error The error message, when available.
     * @return The structured item result.
     */
    private Map<String, Object> itemResult(String entityId, String status, String state, String error) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("entityId", entityId);
        payload.put("status", status);
        if (state != null) {
            payload.put("state", state);
        }
        if (error != null) {
            payload.put("error", error);
        }
        return payload;
    }

    /**
     * Anonymizes a JSON node recursively using field-name heuristics.
     *
     * @param source The source JSON node.
     * @return The anonymized JSON node.
     */
    private JsonNode anonymizeNode(JsonNode source) {
        if (source == null || source.isNull()) {
            return source;
        }
        if (source.isObject()) {
            ObjectNode result = objectMapper.createObjectNode();
            source.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                if (isSensitiveField(key)) {
                    result.set(key, redactedValue(value));
                } else {
                    result.set(key, anonymizeNode(value));
                }
            });
            return result;
        }
        if (source.isArray()) {
            ArrayNode result = objectMapper.createArrayNode();
            source.forEach(item -> result.add(anonymizeNode(item)));
            return result;
        }
        return source;
    }

    /**
     * Determines whether a JSON field should be treated as sensitive.
     *
     * @param fieldName The field name to evaluate.
     * @return {@code true} when the field should be anonymized.
     */
    private boolean isSensitiveField(String fieldName) {
        String normalized = fieldName == null ? "" : fieldName.toLowerCase(Locale.ROOT);
        return normalized.contains("name")
                || normalized.contains("email")
                || normalized.contains("phone")
                || normalized.contains("address")
                || normalized.contains("latitude")
                || normalized.contains("longitude")
                || normalized.contains("zip")
                || normalized.contains("postal")
                || normalized.contains("city")
                || normalized.contains("state")
                || normalized.contains("country");
    }

    /**
     * Produces a redacted JSON value that preserves the original shape.
     *
     * @param source The source JSON value.
     * @return The redacted JSON value.
     */
    private JsonNode redactedValue(JsonNode source) {
        if (source == null || source.isNull()) {
            return source;
        }
        if (source.isNumber()) {
            return objectMapper.getNodeFactory().numberNode(0);
        }
        if (source.isBoolean()) {
            return objectMapper.getNodeFactory().booleanNode(false);
        }
        if (source.isArray()) {
            return objectMapper.createArrayNode();
        }
        if (source.isObject()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.getNodeFactory().textNode("REDACTED");
    }

    /**
     * Maps a recovery record entity to its DTO representation.
     *
     * @param record The recovery record entity.
     * @return The DTO representation.
     */
    private RecoveryRecordDto toRecordDto(RecoveryRecord record) {
        return new RecoveryRecordDto(
                record.getId(),
                record.getTenantKey(),
                record.getEntityType(),
                record.getEntityId(),
                record.getDisplayName(),
                record.getLifecycleState(),
                record.getCurrentVersionNumber(),
                record.isBackupVerified(),
                record.isAnonymized(),
                record.getRetentionUntil(),
                record.getLegalHoldUntil(),
                record.getDeletedAt(),
                record.getArchivedAt(),
                record.getInactivatedAt(),
                record.getRestoredAt(),
                record.getPurgedAt(),
                record.getLastActionBy(),
                record.getLastReason(),
                record.getUpdatedAt()
        );
    }

    /**
     * Maps a recovery version entity to its DTO representation.
     *
     * @param version The recovery version entity.
     * @return The DTO representation.
     */
    private RecoveryVersionDto toVersionDto(RecoveryVersion version) {
        return new RecoveryVersionDto(
                version.getId(),
                version.getVersionNumber(),
                version.getActionType(),
                version.getLifecycleStateAfter(),
                version.getActor(),
                version.getReason(),
                version.isBackupSnapshot(),
                version.isAnonymized(),
                version.getRestoredFromAt(),
                version.getCapturedAt(),
                version.getMetadataJson(),
                version.getSnapshotJson()
        );
    }

    /**
     * Maps a recovery audit-log entity to its DTO representation.
     *
     * @param auditLog The recovery audit-log entity.
     * @return The DTO representation.
     */
    private RecoveryAuditLogDto toAuditLogDto(RecoveryAuditLog auditLog) {
        return new RecoveryAuditLogDto(
                auditLog.getId(),
                auditLog.getTenantKey(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getActionType(),
                auditLog.getActionStatus(),
                auditLog.getActor(),
                auditLog.getCorrelationId(),
                auditLog.getMessage(),
                auditLog.getMetadataJson(),
                auditLog.getOccurredAt()
        );
    }

    /**
     * Maps a recovery action-job entity to its DTO representation.
     *
     * @param job The recovery action-job entity.
     * @return The DTO representation.
     */
    private RecoveryActionJobDto toJobDto(RecoveryActionJob job) {
        return new RecoveryActionJobDto(
                job.getId(),
                job.getTenantKey(),
                job.getEntityType(),
                job.getActionType(),
                job.getStatus(),
                job.getRequestedBy(),
                job.isDryRun(),
                job.getTotalItems(),
                job.getProcessedItems(),
                job.getSuccessItems(),
                job.getFailedItems(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getValidationSummaryJson(),
                job.getResultSummaryJson(),
                job.getErrorSummary(),
                job.getUpdatedAt()
        );
    }
}

package com.noura.platform.service.impl.recovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.config.RecoveryProperties;
import com.noura.platform.domain.entity.RecoveryActionJob;
import com.noura.platform.domain.enums.RecoveryJobStatus;
import com.noura.platform.dto.recovery.RecoveryActionJobDto;
import com.noura.platform.dto.recovery.RecoveryActionRequest;
import com.noura.platform.dto.recovery.RecoveryBulkActionRequest;
import com.noura.platform.repository.RecoveryActionJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Processes bulk recovery jobs without holding a long-running database transaction.
 */
@Service
public class RecoveryJobRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryJobRunner.class);

    private final RecoveryActionJobRepository recoveryActionJobRepository;
    private final RecoveryGovernanceServiceImpl recoveryGovernanceService;
    private final ObjectMapper objectMapper;
    private final RecoveryEventStreamService recoveryEventStreamService;
    private final RecoveryProperties recoveryProperties;
    private final RecoverySlackAlertService recoverySlackAlertService;

    public RecoveryJobRunner(
            RecoveryActionJobRepository recoveryActionJobRepository,
            RecoveryGovernanceServiceImpl recoveryGovernanceService,
            ObjectMapper objectMapper,
            RecoveryEventStreamService recoveryEventStreamService,
            RecoveryProperties recoveryProperties,
            RecoverySlackAlertService recoverySlackAlertService
    ) {
        this.recoveryActionJobRepository = recoveryActionJobRepository;
        this.recoveryGovernanceService = recoveryGovernanceService;
        this.objectMapper = objectMapper;
        this.recoveryEventStreamService = recoveryEventStreamService;
        this.recoveryProperties = recoveryProperties;
        this.recoverySlackAlertService = recoverySlackAlertService;
    }

    public void processJob(UUID jobId, RecoveryBulkActionRequest request, String actor, String tenantKey) {
        RecoveryActionJob job = recoveryActionJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("RECOVERY_JOB_NOT_FOUND", "Recovery action job not found."));
        if (!tenantKey.equals(job.getTenantKey())) {
            throw new NotFoundException("RECOVERY_JOB_NOT_FOUND", "Recovery action job not found.");
        }

        if (job.getStatus() == RecoveryJobStatus.CANCEL_REQUESTED) {
            markCancelled(job, "Cancelled before execution.", List.of());
            return;
        }

        job.setStatus(RecoveryJobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        job.setErrorSummary(null);
        job = recoveryActionJobRepository.save(job);
        publishJob(job);

        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        List<String> entityIds = sanitizeEntityIds(request.entityIds());

        for (String entityId : entityIds) {
            RecoveryJobStatus status = currentStatus(jobId);
            if (status == RecoveryJobStatus.CANCEL_REQUESTED) {
                LOGGER.info("Recovery job {} cancel requested; stopping after {} processed items.", jobId, results.size());
                job = refresh(jobId, tenantKey);
                job.setProcessedItems(results.size());
                job.setSuccessItems(successCount);
                job.setFailedItems(failedCount);
                markCancelled(job, "Cancelled by operator.", results);
                return;
            }

            try {
                if (request.dryRun()) {
                    recoveryGovernanceService.validateActionInternal(
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
                    var result = recoveryGovernanceService.applyActionInternal(
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

            job = refresh(jobId, tenantKey);
            job.setProcessedItems(results.size());
            job.setSuccessItems(successCount);
            job.setFailedItems(failedCount);
            job = recoveryActionJobRepository.save(job);
            publishJob(job);
        }

        job = refresh(jobId, tenantKey);
        job.setCompletedAt(Instant.now());
        job.setResultSummaryJson(toJson(results));
        if (failedCount == 0) {
            job.setStatus(RecoveryJobStatus.COMPLETED);
        } else if (successCount == 0) {
            job.setStatus(RecoveryJobStatus.FAILED);
        } else {
            job.setStatus(RecoveryJobStatus.PARTIAL_SUCCESS);
        }
        job = recoveryActionJobRepository.save(job);
        publishJob(job);

        if (!job.isDryRun()
                && (job.getStatus() == RecoveryJobStatus.FAILED || job.getStatus() == RecoveryJobStatus.PARTIAL_SUCCESS)
                && recoveryProperties.getApprovalRequiredActions().contains(job.getActionType())) {
            Object changeTicket = request.metadata() == null ? null : request.metadata().get("changeTicket");
            Object correlationId = request.metadata() == null ? null : request.metadata().get("correlationId");
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("jobId", String.valueOf(job.getId()));
            context.put("status", String.valueOf(job.getStatus()));
            context.put("entityType", job.getEntityType() == null ? "" : job.getEntityType());
            context.put("actionType", String.valueOf(job.getActionType()));
            context.put("requestedBy", job.getRequestedBy() == null ? "" : job.getRequestedBy());
            context.put("processed", String.valueOf(job.getProcessedItems()));
            context.put("failed", String.valueOf(job.getFailedItems()));
            context.put("changeTicket", changeTicket == null ? "" : String.valueOf(changeTicket));
            context.put("correlationId", correlationId == null ? "" : String.valueOf(correlationId));

            recoverySlackAlertService.notifyHighImpact(
                    "Bulk recovery job completed with failures",
                    context
            );
        }
    }

    private void markCancelled(RecoveryActionJob job, String reason, List<Map<String, Object>> results) {
        job.setStatus(RecoveryJobStatus.CANCELLED);
        job.setCompletedAt(Instant.now());
        job.setErrorSummary(reason);
        if (results != null && !results.isEmpty()) {
            job.setResultSummaryJson(toJson(results));
        }
        job = recoveryActionJobRepository.save(job);
        publishJob(job);
    }

    private RecoveryJobStatus currentStatus(UUID jobId) {
        return recoveryActionJobRepository.findById(jobId)
                .map(RecoveryActionJob::getStatus)
                .orElse(RecoveryJobStatus.FAILED);
    }

    private RecoveryActionJob refresh(UUID jobId, String tenantKey) {
        RecoveryActionJob job = recoveryActionJobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("RECOVERY_JOB_NOT_FOUND", "Recovery action job not found."));
        if (!tenantKey.equals(job.getTenantKey())) {
            throw new NotFoundException("RECOVERY_JOB_NOT_FOUND", "Recovery action job not found.");
        }
        return job;
    }

    private static List<String> sanitizeEntityIds(List<String> entityIds) {
        return entityIds == null
                ? List.of()
                : entityIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private static Map<String, Object> itemResult(String entityId, String status, String state, String error) {
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

    private void publishJob(RecoveryActionJob job) {
        if (recoveryEventStreamService == null || job == null) {
            return;
        }
        recoveryEventStreamService.publish("recovery.job", toDto(job));
    }

    private static RecoveryActionJobDto toDto(RecoveryActionJob job) {
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

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize recovery job result payload.", exception);
        }
    }
}

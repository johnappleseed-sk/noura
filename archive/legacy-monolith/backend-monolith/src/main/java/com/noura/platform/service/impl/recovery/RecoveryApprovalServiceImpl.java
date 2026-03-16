package com.noura.platform.service.impl.recovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.common.exception.BadRequestException;
import com.noura.platform.common.exception.ForbiddenException;
import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.config.RecoveryProperties;
import com.noura.platform.domain.entity.RecoveryActionApproval;
import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.domain.enums.RecoveryApprovalKind;
import com.noura.platform.domain.enums.RecoveryApprovalStatus;
import com.noura.platform.dto.recovery.RecoveryActionJobDto;
import com.noura.platform.dto.recovery.RecoveryActionRequest;
import com.noura.platform.dto.recovery.RecoveryApprovalRequestDto;
import com.noura.platform.dto.recovery.RecoveryBulkActionRequest;
import com.noura.platform.repository.RecoveryActionApprovalRepository;
import com.noura.platform.service.recovery.RecoverableEntityRegistry;
import com.noura.platform.service.recovery.RecoveryApprovalService;
import com.noura.platform.service.recovery.TenantScopeResolver;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class RecoveryApprovalServiceImpl implements RecoveryApprovalService {

    private static final String META_CHANGE_TICKET = "changeTicket";

    private final RecoveryActionApprovalRepository approvalRepository;
    private final TenantScopeResolver tenantScopeResolver;
    private final RecoverableEntityRegistry recoverableEntityRegistry;
    private final RecoveryGovernanceServiceImpl recoveryGovernanceService;
    private final RecoveryProperties recoveryProperties;
    private final ObjectMapper objectMapper;
    private final RecoveryEventStreamService recoveryEventStreamService;
    private final RecoverySlackAlertService recoverySlackAlertService;

    public RecoveryApprovalServiceImpl(
            RecoveryActionApprovalRepository approvalRepository,
            TenantScopeResolver tenantScopeResolver,
            RecoverableEntityRegistry recoverableEntityRegistry,
            RecoveryGovernanceServiceImpl recoveryGovernanceService,
            RecoveryProperties recoveryProperties,
            ObjectMapper objectMapper,
            RecoveryEventStreamService recoveryEventStreamService,
            RecoverySlackAlertService recoverySlackAlertService
    ) {
        this.approvalRepository = approvalRepository;
        this.tenantScopeResolver = tenantScopeResolver;
        this.recoverableEntityRegistry = recoverableEntityRegistry;
        this.recoveryGovernanceService = recoveryGovernanceService;
        this.recoveryProperties = recoveryProperties;
        this.objectMapper = objectMapper;
        this.recoveryEventStreamService = recoveryEventStreamService;
        this.recoverySlackAlertService = recoverySlackAlertService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecoveryApprovalRequestDto> listApprovalRequests(
            String entityType,
            RecoveryActionType actionType,
            RecoveryApprovalStatus status,
            String query,
            Pageable pageable
    ) {
        String tenantKey = tenantScopeResolver.resolveCurrentTenant();
        return approvalRepository.findAll(buildApprovalSpec(tenantKey, entityType, actionType, status, query), pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional
    public RecoveryApprovalRequestDto requestActionApproval(RecoveryActionRequest request, String actor) {
        String tenantKey = tenantScopeResolver.resolveCurrentTenant();
        String normalizedEntityType = recoverableEntityRegistry.normalize(request.entityType());
        recoverableEntityRegistry.getRequiredAdapter(normalizedEntityType);

        enforceApprovalRequired(request.actionType());
        enforceReasonAndTicket(request.reason(), extractChangeTicket(request.metadata()));

        RecoveryActionApproval approval = new RecoveryActionApproval();
        approval.setTenantKey(tenantKey);
        approval.setRequestKind(RecoveryApprovalKind.ACTION);
        approval.setEntityType(normalizedEntityType);
        approval.setEntityId(request.entityId().trim());
        approval.setActionType(request.actionType());
        approval.setStatus(RecoveryApprovalStatus.PENDING);
        approval.setRequestedItems(1);
        approval.setReason(normalizeReason(request.reason()));
        approval.setChangeTicket(extractChangeTicket(request.metadata()));
        approval.setRequestedBy(actor);
        approval.setRequestedAt(Instant.now());
        approval.setRequestPayloadJson(toJson(request));

        RecoveryActionApproval saved = approvalRepository.save(approval);
        RecoveryApprovalRequestDto dto = toDto(saved);
        recoveryEventStreamService.publish("recovery.approval", dto);
        return dto;
    }

    @Override
    @Transactional
    public RecoveryApprovalRequestDto requestBulkApproval(RecoveryBulkActionRequest request, String actor) {
        if (request.dryRun()) {
            throw new BadRequestException("RECOVERY_APPROVAL_DRY_RUN_UNSUPPORTED", "Bulk approvals are only supported for non-dry-run actions.");
        }

        String tenantKey = tenantScopeResolver.resolveCurrentTenant();
        String normalizedEntityType = recoverableEntityRegistry.normalize(request.entityType());
        recoverableEntityRegistry.getRequiredAdapter(normalizedEntityType);

        enforceApprovalRequired(request.actionType());
        enforceReasonAndTicket(request.reason(), extractChangeTicket(request.metadata()));

        List<String> entityIds = request.entityIds().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (entityIds.isEmpty()) {
            throw new BadRequestException("RECOVERY_BULK_IDS_REQUIRED", "Bulk approvals require at least one entity id.");
        }

        RecoveryBulkActionRequest sanitizedRequest = new RecoveryBulkActionRequest(
                request.entityType(),
                request.actionType(),
                entityIds,
                request.reason(),
                false,
                request.restoreTo(),
                request.legalHoldUntil(),
                request.retentionDays(),
                request.metadata()
        );

        RecoveryActionApproval approval = new RecoveryActionApproval();
        approval.setTenantKey(tenantKey);
        approval.setRequestKind(RecoveryApprovalKind.BULK_ACTION);
        approval.setEntityType(normalizedEntityType);
        approval.setEntityId(null);
        approval.setActionType(request.actionType());
        approval.setStatus(RecoveryApprovalStatus.PENDING);
        approval.setRequestedItems(entityIds.size());
        approval.setReason(normalizeReason(request.reason()));
        approval.setChangeTicket(extractChangeTicket(request.metadata()));
        approval.setRequestedBy(actor);
        approval.setRequestedAt(Instant.now());
        approval.setRequestPayloadJson(toJson(sanitizedRequest));

        RecoveryActionApproval saved = approvalRepository.save(approval);
        RecoveryApprovalRequestDto dto = toDto(saved);
        recoveryEventStreamService.publish("recovery.approval", dto);
        return dto;
    }

    @Override
    @Transactional
    public RecoveryApprovalRequestDto approve(UUID approvalId, String reviewer, String reviewerNotes) {
        String tenantKey = tenantScopeResolver.resolveCurrentTenant();
        RecoveryActionApproval approval = approvalRepository.findByIdAndTenantKey(approvalId, tenantKey)
                .orElseThrow(() -> new NotFoundException("RECOVERY_APPROVAL_NOT_FOUND", "Recovery approval request not found."));

        if (approval.getStatus() != RecoveryApprovalStatus.PENDING) {
            throw new BadRequestException("RECOVERY_APPROVAL_NOT_PENDING", "Only pending approval requests can be approved.");
        }
        enforceFourEyes(approval.getRequestedBy(), reviewer);

        approval.setStatus(RecoveryApprovalStatus.APPROVED);
        approval.setReviewedBy(reviewer);
        approval.setReviewedAt(Instant.now());
        approval.setReviewerNotes(normalizeNotes(reviewerNotes));
        approvalRepository.save(approval);

        try {
            executeApproval(approval, reviewer, tenantKey);
            approval.setStatus(RecoveryApprovalStatus.EXECUTED);
            approval.setExecutedAt(Instant.now());
            approval.setExecutionError(null);
        } catch (RuntimeException exception) {
            approval.setStatus(RecoveryApprovalStatus.EXECUTION_FAILED);
            approval.setExecutedAt(Instant.now());
            approval.setExecutionError(truncate(exception.getMessage(), 1000));
        }

        if (approval.getStatus() == RecoveryApprovalStatus.EXECUTION_FAILED) {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("approvalId", String.valueOf(approval.getId()));
            context.put("actionType", String.valueOf(approval.getActionType()));
            context.put("entityType", approval.getEntityType() == null ? "" : approval.getEntityType());
            context.put("entityId", approval.getEntityId() == null ? "" : approval.getEntityId());
            context.put("requestedBy", approval.getRequestedBy() == null ? "" : approval.getRequestedBy());
            context.put("reviewedBy", approval.getReviewedBy() == null ? "" : approval.getReviewedBy());
            context.put("changeTicket", approval.getChangeTicket() == null ? "" : approval.getChangeTicket());
            context.put("error", approval.getExecutionError() == null ? "" : approval.getExecutionError());

            recoverySlackAlertService.notifyHighImpact(
                    "Approval execution failed",
                    context
            );
        }

        RecoveryActionApproval saved = approvalRepository.save(approval);
        RecoveryApprovalRequestDto dto = toDto(saved);
        recoveryEventStreamService.publish("recovery.approval", dto);
        return dto;
    }

    @Override
    @Transactional
    public RecoveryApprovalRequestDto reject(UUID approvalId, String reviewer, String reviewerNotes) {
        String tenantKey = tenantScopeResolver.resolveCurrentTenant();
        RecoveryActionApproval approval = approvalRepository.findByIdAndTenantKey(approvalId, tenantKey)
                .orElseThrow(() -> new NotFoundException("RECOVERY_APPROVAL_NOT_FOUND", "Recovery approval request not found."));

        if (approval.getStatus() != RecoveryApprovalStatus.PENDING) {
            throw new BadRequestException("RECOVERY_APPROVAL_NOT_PENDING", "Only pending approval requests can be rejected.");
        }
        enforceFourEyes(approval.getRequestedBy(), reviewer);

        approval.setStatus(RecoveryApprovalStatus.REJECTED);
        approval.setReviewedBy(reviewer);
        approval.setReviewedAt(Instant.now());
        approval.setReviewerNotes(normalizeNotes(reviewerNotes));
        approvalRepository.save(approval);
        RecoveryApprovalRequestDto dto = toDto(approval);
        recoveryEventStreamService.publish("recovery.approval", dto);
        return dto;
    }

    private void executeApproval(RecoveryActionApproval approval, String actor, String tenantKey) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("source", "recovery-approval");
        meta.put("approvalId", String.valueOf(approval.getId()));
        meta.put("requestedBy", approval.getRequestedBy());
        meta.put("reviewedBy", actor);
        if (approval.getChangeTicket() != null) {
            meta.put(META_CHANGE_TICKET, approval.getChangeTicket());
        }

        if (approval.getRequestKind() == RecoveryApprovalKind.ACTION) {
            RecoveryActionRequest request = fromJson(approval.getRequestPayloadJson(), RecoveryActionRequest.class);
            RecoveryActionRequest enriched = new RecoveryActionRequest(
                    request.entityType(),
                    request.entityId(),
                    request.actionType(),
                    request.reason(),
                    request.restoreTo(),
                    request.legalHoldUntil(),
                    request.retentionDays(),
                    mergeMeta(request.metadata(), meta)
            );
            recoveryGovernanceService.applyActionInternal(enriched, actor, tenantKey);
            return;
        }

        if (approval.getRequestKind() == RecoveryApprovalKind.BULK_ACTION) {
            RecoveryBulkActionRequest request = fromJson(approval.getRequestPayloadJson(), RecoveryBulkActionRequest.class);
            RecoveryBulkActionRequest enriched = new RecoveryBulkActionRequest(
                    request.entityType(),
                    request.actionType(),
                    request.entityIds(),
                    request.reason(),
                    request.dryRun(),
                    request.restoreTo(),
                    request.legalHoldUntil(),
                    request.retentionDays(),
                    mergeMeta(request.metadata(), meta)
            );
            RecoveryActionJobDto job = recoveryGovernanceService.submitBulkActionInternal(enriched, actor, tenantKey, true);
            approval.setExecutedJobId(job.id());
            return;
        }

        throw new BadRequestException("RECOVERY_APPROVAL_KIND_UNSUPPORTED", "Unsupported approval request type: " + approval.getRequestKind());
    }

    private void enforceApprovalRequired(RecoveryActionType actionType) {
        if (recoveryProperties.getApprovalRequiredActions().contains(actionType)) {
            return;
        }
        throw new BadRequestException("RECOVERY_APPROVAL_NOT_REQUIRED", "Approval workflow is not configured for this action type.");
    }

    private void enforceReasonAndTicket(String reason, String changeTicket) {
        String normalizedReason = normalizeReason(reason);
        if (normalizedReason.length() < recoveryProperties.getMinReasonLength()) {
            throw new BadRequestException("RECOVERY_REASON_REQUIRED", "Provide a business reason (at least " + recoveryProperties.getMinReasonLength() + " characters).");
        }
        if (changeTicket == null || changeTicket.isBlank()) {
            throw new BadRequestException("RECOVERY_CHANGE_TICKET_REQUIRED", "changeTicket is required for this approval request.");
        }
    }

    private void enforceFourEyes(String requestedBy, String reviewer) {
        if (requestedBy == null || requestedBy.isBlank() || reviewer == null || reviewer.isBlank()) {
            return;
        }
        if (requestedBy.trim().equalsIgnoreCase(reviewer.trim())) {
            throw new ForbiddenException("RECOVERY_APPROVAL_SELF_REVIEW", "The requesting operator cannot approve or reject their own request.");
        }
    }

    private Specification<RecoveryActionApproval> buildApprovalSpec(
            String tenantKey,
            String entityType,
            RecoveryActionType actionType,
            RecoveryApprovalStatus status,
            String query
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
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("entityType")), like),
                        builder.like(builder.lower(root.get("entityId")), like),
                        builder.like(builder.lower(root.get("reason")), like),
                        builder.like(builder.lower(root.get("changeTicket")), like),
                        builder.like(builder.lower(root.get("requestedBy")), like),
                        builder.like(builder.lower(root.get("reviewedBy")), like)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private RecoveryApprovalRequestDto toDto(RecoveryActionApproval approval) {
        return new RecoveryApprovalRequestDto(
                approval.getId(),
                approval.getTenantKey(),
                approval.getRequestKind(),
                approval.getEntityType(),
                approval.getEntityId(),
                approval.getActionType(),
                approval.getStatus(),
                approval.getRequestedItems(),
                approval.getReason(),
                approval.getChangeTicket(),
                approval.getRequestedBy(),
                approval.getRequestedAt(),
                approval.getReviewedBy(),
                approval.getReviewedAt(),
                approval.getReviewerNotes(),
                approval.getExecutedJobId(),
                approval.getExecutedAt(),
                approval.getExecutionError(),
                approval.getUpdatedAt()
        );
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize recovery approval payload.", exception);
        }
    }

    private <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to parse recovery approval payload.", exception);
        }
    }

    private static Map<String, Object> mergeMeta(Map<String, Object> original, Map<String, Object> extra) {
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
        if (original != null) {
            merged.putAll(original);
        }
        if (extra != null) {
            merged.putAll(extra);
        }
        return merged;
    }

    private static String extractChangeTicket(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        Object value = metadata.get(META_CHANGE_TICKET);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private static String normalizeReason(String reason) {
        String value = Optional.ofNullable(reason).orElse("").trim();
        return truncate(value, 1000);
    }

    private static String normalizeNotes(String notes) {
        String value = Optional.ofNullable(notes).orElse("").trim();
        return truncate(value, 1000);
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
}

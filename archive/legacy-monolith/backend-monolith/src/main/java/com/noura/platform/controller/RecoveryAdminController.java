package com.noura.platform.controller;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.common.api.PaginationUtils;
import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.domain.enums.RecoveryApprovalStatus;
import com.noura.platform.domain.enums.RecoveryJobStatus;
import com.noura.platform.domain.enums.RecoveryLifecycleState;
import com.noura.platform.dto.recovery.RecoveryActionJobDto;
import com.noura.platform.dto.recovery.RecoveryActionRequest;
import com.noura.platform.dto.recovery.RecoveryActionResultDto;
import com.noura.platform.dto.recovery.RecoveryApprovalRequestDto;
import com.noura.platform.dto.recovery.RecoveryApprovalReviewRequest;
import com.noura.platform.dto.recovery.RecoveryAuditLogDto;
import com.noura.platform.dto.recovery.RecoveryBulkActionRequest;
import com.noura.platform.dto.recovery.RecoveryRecordDto;
import com.noura.platform.dto.recovery.RecoveryVersionDto;
import com.noura.platform.service.impl.recovery.RecoveryEventStreamService;
import com.noura.platform.service.recovery.RecoveryApprovalService;
import com.noura.platform.service.recovery.RecoveryGovernanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Exposes the admin recovery center APIs for governed destructive actions and recovery workflows.
 */
@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("${app.api.version-prefix:/api/v1}/admin/recovery")
public class RecoveryAdminController {

    private final RecoveryGovernanceService recoveryGovernanceService;
    private final RecoveryApprovalService recoveryApprovalService;
    private final RecoveryEventStreamService recoveryEventStreamService;

    /**
     * Creates a new recovery admin controller.
     *
     * @param recoveryGovernanceService The recovery governance service.
     */
    public RecoveryAdminController(
            RecoveryGovernanceService recoveryGovernanceService,
            RecoveryApprovalService recoveryApprovalService,
            RecoveryEventStreamService recoveryEventStreamService
    ) {
        this.recoveryGovernanceService = recoveryGovernanceService;
        this.recoveryApprovalService = recoveryApprovalService;
        this.recoveryEventStreamService = recoveryEventStreamService;
    }

    /**
     * Lists governed recovery records.
     *
     * @param entityType The optional business entity type filter.
     * @param lifecycleState The optional lifecycle-state filter.
     * @param query The optional free-text filter.
     * @param page The pagination configuration.
     * @param size The requested page size.
     * @param sortBy The requested sort field.
     * @param direction The requested sort direction.
     * @param http The current HTTP request.
     * @return The paginated recovery record response.
     */
    @GetMapping("/records")
    public ApiResponse<PageResponse<RecoveryRecordDto>> listRecords(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) RecoveryLifecycleState lifecycleState,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<RecoveryRecordDto> data = recoveryGovernanceService.listRecords(entityType, lifecycleState, query, pageable);
        return ApiResponse.ok("Recovery records", PageResponse.from(data), http.getRequestURI());
    }

    /**
     * Lists recovery versions for a governed entity.
     *
     * @param entityType The business entity type.
     * @param entityId The business entity identifier.
     * @param http The current HTTP request.
     * @return The ordered recovery version list.
     */
    @GetMapping("/records/{entityType}/{entityId}/versions")
    public ApiResponse<List<RecoveryVersionDto>> listVersions(
            @PathVariable String entityType,
            @PathVariable String entityId,
            HttpServletRequest http
    ) {
        return ApiResponse.ok("Recovery versions", recoveryGovernanceService.listVersions(entityType, entityId), http.getRequestURI());
    }

    /**
     * Lists recovery audit logs.
     *
     * @param entityType The optional business entity type filter.
     * @param actionType The optional action-type filter.
     * @param actionStatus The optional action-status filter.
     * @param query The optional free-text filter.
     * @param errorsOnly Whether to only include non-successful events.
     * @param page The pagination configuration.
     * @param size The requested page size.
     * @param sortBy The requested sort field.
     * @param direction The requested sort direction.
     * @param http The current HTTP request.
     * @return The paginated recovery audit-log response.
     */
    @GetMapping("/audit-logs")
    public ApiResponse<PageResponse<RecoveryAuditLogDto>> listAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) RecoveryActionType actionType,
            @RequestParam(required = false) String actionStatus,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean errorsOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "occurredAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<RecoveryAuditLogDto> data = recoveryGovernanceService.listAuditLogs(entityType, actionType, actionStatus, query, errorsOnly, pageable);
        return ApiResponse.ok("Recovery audit logs", PageResponse.from(data), http.getRequestURI());
    }

    /**
     * Lists bulk destructive-action jobs.
     *
     * @param entityType The optional business entity type filter.
     * @param status The optional job-status filter.
     * @param query The optional free-text filter.
     * @param errorsOnly Whether to only include failed/partial jobs.
     * @param page The pagination configuration.
     * @param size The requested page size.
     * @param sortBy The requested sort field.
     * @param direction The requested sort direction.
     * @param http The current HTTP request.
     * @return The paginated recovery job response.
     */
    @GetMapping("/jobs")
    public ApiResponse<PageResponse<RecoveryActionJobDto>> listJobs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) RecoveryJobStatus status,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean errorsOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<RecoveryActionJobDto> data = recoveryGovernanceService.listJobs(entityType, status, query, errorsOnly, pageable);
        return ApiResponse.ok("Recovery action jobs", PageResponse.from(data), http.getRequestURI());
    }

    /**
     * Executes a single governed destructive or recovery action.
     *
     * @param request The requested action payload.
     * @param authentication The current authentication.
     * @param http The current HTTP request.
     * @return The action result response.
     */
    @PostMapping("/actions")
    public ApiResponse<RecoveryActionResultDto> applyAction(
            @Valid @RequestBody RecoveryActionRequest request,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String actor = authentication == null ? null : authentication.getName();
        return ApiResponse.ok("Recovery action applied", recoveryGovernanceService.applyAction(request, actor), http.getRequestURI());
    }

    /**
     * Enqueues a bulk governed destructive-action job.
     *
     * @param request The requested bulk-action payload.
     * @param authentication The current authentication.
     * @param http The current HTTP request.
     * @return The created job response.
     */
    @PostMapping("/bulk-actions")
    public ResponseEntity<ApiResponse<RecoveryActionJobDto>> submitBulkAction(
            @Valid @RequestBody RecoveryBulkActionRequest request,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String actor = authentication == null ? null : authentication.getName();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("Recovery action job queued", recoveryGovernanceService.submitBulkAction(request, actor), http.getRequestURI()));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return recoveryEventStreamService.subscribe();
    }

    @PostMapping("/jobs/{jobId}/cancel")
    public ApiResponse<RecoveryActionJobDto> cancelJob(
            @PathVariable java.util.UUID jobId,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String actor = authentication == null ? null : authentication.getName();
        return ApiResponse.ok("Recovery job cancel requested", recoveryGovernanceService.requestJobCancel(jobId, actor), http.getRequestURI());
    }

    @PostMapping("/jobs/{jobId}/retry")
    public ResponseEntity<ApiResponse<RecoveryActionJobDto>> retryJob(
            @PathVariable java.util.UUID jobId,
            @RequestParam(defaultValue = "true") boolean failedOnly,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String actor = authentication == null ? null : authentication.getName();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("Recovery job retry queued", recoveryGovernanceService.retryJob(jobId, failedOnly, actor), http.getRequestURI()));
    }

    @GetMapping(value = "/jobs/{jobId}/failure-report", produces = "text/csv")
    public ResponseEntity<String> failureReport(
            @PathVariable java.util.UUID jobId
    ) {
        String csv = recoveryGovernanceService.failureReportCsv(jobId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"recovery-job-" + jobId + "-failures.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/approval-requests")
    public ApiResponse<PageResponse<RecoveryApprovalRequestDto>> listApprovalRequests(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) RecoveryActionType actionType,
            @RequestParam(required = false) RecoveryApprovalStatus status,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "requestedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = PaginationUtils.pageOf(page, size, sortBy, direction);
        Page<RecoveryApprovalRequestDto> data = recoveryApprovalService.listApprovalRequests(entityType, actionType, status, query, pageable);
        return ApiResponse.ok("Recovery approval requests", PageResponse.from(data), http.getRequestURI());
    }

    @PostMapping("/approval-requests")
    public ResponseEntity<ApiResponse<RecoveryApprovalRequestDto>> requestActionApproval(
            @Valid @RequestBody RecoveryActionRequest request,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String actor = authentication == null ? null : authentication.getName();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("Approval requested", recoveryApprovalService.requestActionApproval(request, actor), http.getRequestURI()));
    }

    @PostMapping("/approval-requests/bulk")
    public ResponseEntity<ApiResponse<RecoveryApprovalRequestDto>> requestBulkApproval(
            @Valid @RequestBody RecoveryBulkActionRequest request,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String actor = authentication == null ? null : authentication.getName();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("Bulk approval requested", recoveryApprovalService.requestBulkApproval(request, actor), http.getRequestURI()));
    }

    @PostMapping("/approval-requests/{approvalId}/approve")
    public ApiResponse<RecoveryApprovalRequestDto> approve(
            @PathVariable java.util.UUID approvalId,
            @RequestBody(required = false) RecoveryApprovalReviewRequest request,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String actor = authentication == null ? null : authentication.getName();
        String notes = request == null ? null : request.reviewerNotes();
        return ApiResponse.ok("Approval executed", recoveryApprovalService.approve(approvalId, actor, notes), http.getRequestURI());
    }

    @PostMapping("/approval-requests/{approvalId}/reject")
    public ApiResponse<RecoveryApprovalRequestDto> reject(
            @PathVariable java.util.UUID approvalId,
            @RequestBody(required = false) RecoveryApprovalReviewRequest request,
            Authentication authentication,
            HttpServletRequest http
    ) {
        String actor = authentication == null ? null : authentication.getName();
        String notes = request == null ? null : request.reviewerNotes();
        return ApiResponse.ok("Approval rejected", recoveryApprovalService.reject(approvalId, actor, notes), http.getRequestURI());
    }
}

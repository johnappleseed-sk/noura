package com.noura.platform.inventory.api;

import com.noura.platform.common.api.ApiResponse;
import com.noura.platform.common.api.PageResponse;
import com.noura.platform.inventory.dto.audit.AuditLogFilter;
import com.noura.platform.inventory.dto.audit.AuditLogResponse;
import com.noura.platform.inventory.service.AuditLogQueryService;
import com.noura.platform.inventory.support.InventoryPageRequestFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Set;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/v1/audit-logs")
public class AuditLogController {

    private static final Set<String> ALLOWED_SORTS = Set.of("occurredAt", "entityType", "actionCode", "actorEmail");

    private final AuditLogQueryService auditLogQueryService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<AuditLogResponse>> listAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String actionCode,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) Instant occurredFrom,
            @RequestParam(required = false) Instant occurredTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "occurredAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest http
    ) {
        Pageable pageable = InventoryPageRequestFactory.of(page, size, sortBy, direction, ALLOWED_SORTS, "occurredAt");
        Page<AuditLogResponse> result = auditLogQueryService.listAuditLogs(
                new AuditLogFilter(entityType, entityId, actionCode, actorEmail, occurredFrom, occurredTo),
                pageable
        );
        return ApiResponse.ok("Audit logs", PageResponse.from(result), http.getRequestURI());
    }
}

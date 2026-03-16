package com.noura.platform.service;

import com.noura.platform.dto.audit.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuditLogService {
    Page<AuditLogResponse> listAuditLogs(String actor, String actionCode, String entityType, Pageable pageable);

    void record(AuditLogCommand command);

    record AuditLogCommand(
            UUID actorUserId,
            String actorUsername,
            String actionCode,
            String entityType,
            UUID entityId,
            String oldValueJson,
            String newValueJson,
            String requestPath,
            String requestMethod,
            String ipAddress
    ) {
    }
}

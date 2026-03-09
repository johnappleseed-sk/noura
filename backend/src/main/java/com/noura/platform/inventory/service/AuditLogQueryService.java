package com.noura.platform.inventory.service;

import com.noura.platform.inventory.dto.audit.AuditLogFilter;
import com.noura.platform.inventory.dto.audit.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogQueryService {

    Page<AuditLogResponse> listAuditLogs(AuditLogFilter filter, Pageable pageable);
}

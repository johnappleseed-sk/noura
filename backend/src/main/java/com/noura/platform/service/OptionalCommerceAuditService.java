package com.noura.platform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OptionalCommerceAuditService {

    private final ObjectProvider<com.noura.platform.commerce.service.AuditEventService> auditEventServiceProvider;

    public void record(
            String actionType,
            String targetType,
            Object targetId,
            Object beforeState,
            Object afterState,
            Object metadata
    ) {
        com.noura.platform.commerce.service.AuditEventService service = auditEventServiceProvider.getIfAvailable();
        if (service == null) {
            return;
        }
        service.record(actionType, targetType, targetId, beforeState, afterState, metadata);
    }
}

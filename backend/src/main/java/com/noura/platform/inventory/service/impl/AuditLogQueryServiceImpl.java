package com.noura.platform.inventory.service.impl;

import com.noura.platform.inventory.domain.AuditLog;
import com.noura.platform.inventory.dto.audit.AuditLogFilter;
import com.noura.platform.inventory.dto.audit.AuditLogResponse;
import com.noura.platform.inventory.repository.AuditLogRepository;
import com.noura.platform.inventory.service.AuditLogQueryService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuditLogQueryServiceImpl implements AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(transactionManager = "inventoryTransactionManager", readOnly = true)
    public Page<AuditLogResponse> listAuditLogs(AuditLogFilter filter, Pageable pageable) {
        AuditLogFilter effectiveFilter = filter == null ? new AuditLogFilter(null, null, null, null, null, null) : filter;
        return auditLogRepository.findAll((root, query, cb) -> {
            java.util.List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(effectiveFilter.entityType())) {
                predicates.add(cb.equal(cb.upper(root.get("entityType")), effectiveFilter.entityType().trim().toUpperCase(Locale.ROOT)));
            }
            if (StringUtils.hasText(effectiveFilter.entityId())) {
                predicates.add(cb.equal(root.get("entityId"), effectiveFilter.entityId().trim()));
            }
            if (StringUtils.hasText(effectiveFilter.actionCode())) {
                predicates.add(cb.equal(cb.upper(root.get("actionCode")), effectiveFilter.actionCode().trim().toUpperCase(Locale.ROOT)));
            }
            if (StringUtils.hasText(effectiveFilter.actorEmail())) {
                predicates.add(cb.equal(cb.lower(root.get("actorEmail")), effectiveFilter.actorEmail().trim().toLowerCase(Locale.ROOT)));
            }
            if (effectiveFilter.occurredFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), effectiveFilter.occurredFrom()));
            }
            if (effectiveFilter.occurredTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), effectiveFilter.occurredTo()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }, pageable).map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActorUser() != null ? auditLog.getActorUser().getId() : null,
                auditLog.getActorEmail(),
                auditLog.getActionCode(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getCorrelationId(),
                auditLog.getBeforeStateJson(),
                auditLog.getAfterStateJson(),
                auditLog.getMetadataJson(),
                auditLog.getIpAddress(),
                auditLog.getUserAgent(),
                auditLog.getEventHash(),
                auditLog.getOccurredAt()
        );
    }
}

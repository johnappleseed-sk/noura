package com.noura.platform.service.impl;

import com.noura.platform.domain.entity.AuditLogEntry;
import com.noura.platform.dto.audit.AuditLogResponse;
import com.noura.platform.repository.AuditLogRepository;
import com.noura.platform.service.AuditLogService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PERM_AUDIT_LOGS_READ') or hasAnyRole('ADMIN','SUPER_ADMIN')")
    public Page<AuditLogResponse> listAuditLogs(String actor, String actionCode, String entityType, Pageable pageable) {
        return auditLogRepository.findAll(buildSpecification(actor, actionCode, entityType), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditLogCommand command) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setActorUserId(command.actorUserId());
        entry.setActorUsername(command.actorUsername());
        entry.setActionCode(command.actionCode());
        entry.setEntityType(command.entityType());
        entry.setEntityId(command.entityId());
        entry.setOldValueJson(command.oldValueJson());
        entry.setNewValueJson(command.newValueJson());
        entry.setRequestPath(command.requestPath());
        entry.setRequestMethod(command.requestMethod());
        entry.setIpAddress(command.ipAddress());
        auditLogRepository.save(entry);
    }

    private Specification<AuditLogEntry> buildSpecification(String actor, String actionCode, String entityType) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (actor != null && !actor.isBlank()) {
                String like = "%" + actor.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("actorUsername")), like));
            }
            if (actionCode != null && !actionCode.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("actionCode")), actionCode.trim().toLowerCase(Locale.ROOT)));
            }
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("entityType")), entityType.trim().toLowerCase(Locale.ROOT)));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private AuditLogResponse toResponse(AuditLogEntry entry) {
        return new AuditLogResponse(
                entry.getId(),
                entry.getActorUserId(),
                entry.getActorUsername(),
                entry.getActionCode(),
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getOldValueJson(),
                entry.getNewValueJson(),
                entry.getRequestPath(),
                entry.getRequestMethod(),
                entry.getIpAddress(),
                entry.getCreatedAt()
        );
    }
}

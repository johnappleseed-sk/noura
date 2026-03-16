package com.noura.platform.inventory.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.platform.inventory.domain.AuditLog;
import com.noura.platform.inventory.domain.BaseUuidEntity;
import com.noura.platform.inventory.domain.IamUser;
import com.noura.platform.inventory.repository.AuditLogRepository;
import com.noura.platform.inventory.repository.IamUserRepository;
import com.noura.platform.inventory.security.InventorySecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryAuditService {

    private final AuditLogRepository auditLogRepository;
    private final IamUserRepository iamUserRepository;
    private final ObjectMapper objectMapper;

    @Transactional(transactionManager = "inventoryTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void recordEntityChange(String action, Object entity, String beforeState, String afterState) {
        if (!(entity instanceof BaseUuidEntity auditable) || auditable.getId() == null) {
            return;
        }
        Instant occurredAt = Instant.now();
        AuditLog auditLog = new AuditLog();
        auditLog.setActorEmail(InventorySecurityContext.currentPrincipal().map(principal -> principal.email()).orElse(null));
        auditLog.setActorUser(resolveActorUser());
        auditLog.setActionCode(entity.getClass().getSimpleName().toUpperCase() + "_" + action);
        auditLog.setEntityType(entity.getClass().getSimpleName());
        auditLog.setEntityId(auditable.getId());
        auditLog.setCorrelationId(currentCorrelationId());
        auditLog.setBeforeStateJson(beforeState);
        auditLog.setAfterStateJson(afterState);
        auditLog.setMetadataJson(serializeMetadata());
        auditLog.setIpAddress(currentIpAddress());
        auditLog.setUserAgent(currentUserAgent());
        auditLog.setOccurredAt(occurredAt);
        auditLog.setEventHash(hash(
                auditLog.getActionCode(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                String.valueOf(beforeState),
                String.valueOf(afterState),
                occurredAt.toString()
        ));
        auditLogRepository.save(auditLog);
    }

    public String serializeEntityState(Object entity) {
        try {
            return objectMapper.writeValueAsString(toAuditMap(entity));
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"AUDIT_SERIALIZATION_FAILED\"}";
        }
    }

    private Map<String, Object> toAuditMap(Object entity) {
        Map<String, Object> state = new LinkedHashMap<>();
        Class<?> currentType = entity.getClass();
        while (currentType != null && currentType != Object.class) {
            for (Field field : currentType.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || Modifier.isTransient(field.getModifiers())
                        || "auditSnapshot".equals(field.getName())) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    state.putIfAbsent(field.getName(), simplifyValue(field.get(entity)));
                } catch (IllegalAccessException ignored) {
                    state.putIfAbsent(field.getName(), null);
                }
            }
            currentType = currentType.getSuperclass();
        }
        return state;
    }

    private Object simplifyValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BaseUuidEntity entity) {
            return entity.getId();
        }
        if (value instanceof Collection<?> collection) {
            List<Object> items = new ArrayList<>();
            for (Object item : collection) {
                items.add(simplifyValue(item));
            }
            return items;
        }
        if (value instanceof Number || value instanceof CharSequence || value instanceof Boolean || value instanceof Enum<?> || value instanceof Instant) {
            return value;
        }
        if (value.getClass().getPackageName().startsWith("java.time")) {
            return value.toString();
        }
        if (value.getClass().getPackageName().startsWith("com.noura.platform.inventory.domain.id")) {
            return value.toString();
        }
        return String.valueOf(value);
    }

    private IamUser resolveActorUser() {
        return InventorySecurityContext.currentPrincipal()
                .flatMap(principal -> iamUserRepository.findByIdAndDeletedAtIsNull(principal.userId()))
                .orElse(null);
    }

    private String serializeMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        InventorySecurityContext.currentPrincipal().ifPresent(principal -> {
            metadata.put("username", principal.username());
            metadata.put("roles", principal.roles());
            metadata.put("permissions", principal.permissions());
        });
        try {
            return metadata.isEmpty() ? null : objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private String currentCorrelationId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        String header = attributes.getRequest().getHeader("X-Correlation-Id");
        return header == null || header.isBlank() ? null : header.trim();
    }

    private String currentIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest().getRemoteAddr();
    }

    private String currentUserAgent() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        String userAgent = attributes.getRequest().getHeader("User-Agent");
        return userAgent == null || userAgent.isBlank() ? null : userAgent.trim();
    }

    private String hash(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                digest.update(value.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '|');
            }
            byte[] hashed = digest.digest();
            StringBuilder builder = new StringBuilder();
            for (byte item : hashed) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}

package com.noura.platform.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "audit_log_entries")
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_username", length = 255)
    private String actorUsername;

    @Column(name = "action_code", nullable = false, length = 120)
    private String actionCode;

    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Lob
    @Column(name = "old_value_json", columnDefinition = "TEXT")
    private String oldValueJson;

    @Lob
    @Column(name = "new_value_json", columnDefinition = "TEXT")
    private String newValueJson;

    @Column(name = "request_path", length = 512)
    private String requestPath;

    @Column(name = "request_method", length = 16)
    private String requestMethod;

    @Column(name = "ip_address", length = 128)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        actionCode = trim(actionCode);
        entityType = trim(entityType);
        actorUsername = trim(actorUsername);
        requestPath = trim(requestPath);
        requestMethod = trim(requestMethod);
        ipAddress = trim(ipAddress);
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

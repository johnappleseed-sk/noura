package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.RecoveryActionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Captures immutable audit events for destructive and recovery operations.
 */
@Getter
@Setter
@Entity
@Table(name = "recovery_audit_logs")
public class RecoveryAuditLog extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String tenantKey;

    @Column(nullable = false, length = 120)
    private String entityType;

    @Column(length = 120)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RecoveryActionType actionType;

    @Column(nullable = false, length = 40)
    private String actionStatus;

    @Column(length = 255)
    private String actor;

    @Column(length = 120)
    private String correlationId;

    @Column(length = 1000)
    private String message;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    @Column(nullable = false)
    private Instant occurredAt;
}

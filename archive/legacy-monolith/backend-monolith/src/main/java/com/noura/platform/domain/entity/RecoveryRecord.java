package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.RecoveryLifecycleState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores the current governed lifecycle state for a recoverable business record.
 */
@Getter
@Setter
@Entity
@Table(name = "recovery_records")
public class RecoveryRecord extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String tenantKey;

    @Column(nullable = false, length = 120)
    private String entityType;

    @Column(nullable = false, length = 120)
    private String entityId;

    @Column(length = 255)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RecoveryLifecycleState lifecycleState = RecoveryLifecycleState.ACTIVE;

    @Column(nullable = false)
    private Integer currentVersionNumber = 0;

    private UUID lastVersionId;

    @Column(nullable = false)
    private boolean backupVerified;

    @Column(nullable = false)
    private boolean anonymized;

    private Instant retentionUntil;

    private Instant legalHoldUntil;

    private Instant deletedAt;

    private Instant archivedAt;

    private Instant inactivatedAt;

    private Instant restoredAt;

    private Instant purgedAt;

    @Column(length = 255)
    private String lastActionBy;

    @Column(length = 1000)
    private String lastReason;
}

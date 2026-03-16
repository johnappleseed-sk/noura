package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.domain.enums.RecoveryApprovalKind;
import com.noura.platform.domain.enums.RecoveryApprovalStatus;
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
 * Persists 4-eyes approval requests for high-impact recovery actions and bulk jobs.
 */
@Getter
@Setter
@Entity
@Table(name = "recovery_action_approvals")
public class RecoveryActionApproval extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String tenantKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RecoveryApprovalKind requestKind;

    @Column(nullable = false, length = 120)
    private String entityType;

    @Column(length = 120)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RecoveryActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RecoveryApprovalStatus status = RecoveryApprovalStatus.PENDING;

    @Column(nullable = false)
    private Integer requestedItems = 1;

    @Column(length = 1000)
    private String reason;

    @Column(length = 255)
    private String changeTicket;

    @Column(length = 255)
    private String requestedBy;

    @Column(nullable = false)
    private Instant requestedAt = Instant.now();

    @Column(length = 255)
    private String reviewedBy;

    private Instant reviewedAt;

    @Column(length = 1000)
    private String reviewerNotes;

    private UUID executedJobId;

    private Instant executedAt;

    @Column(length = 1000)
    private String executionError;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String requestPayloadJson;
}


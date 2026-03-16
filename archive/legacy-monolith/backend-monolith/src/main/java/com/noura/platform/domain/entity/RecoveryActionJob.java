package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.domain.enums.RecoveryJobStatus;
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
 * Represents an orchestrated bulk destructive-action job.
 */
@Getter
@Setter
@Entity
@Table(name = "recovery_action_jobs")
public class RecoveryActionJob extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String tenantKey;

    @Column(nullable = false, length = 120)
    private String entityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RecoveryActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RecoveryJobStatus status = RecoveryJobStatus.VALIDATING;

    @Column(length = 255)
    private String requestedBy;

    @Column(nullable = false)
    private boolean dryRun;

    @Column(nullable = false)
    private Integer totalItems = 0;

    @Column(nullable = false)
    private Integer processedItems = 0;

    @Column(nullable = false)
    private Integer successItems = 0;

    @Column(nullable = false)
    private Integer failedItems = 0;

    private Instant startedAt;

    private Instant completedAt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String validationSummaryJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String requestPayloadJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String resultSummaryJson;

    @Column(length = 1000)
    private String errorSummary;
}

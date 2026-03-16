package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.RecoveryActionType;
import com.noura.platform.domain.enums.RecoveryLifecycleState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores immutable recovery snapshots used for version history, backup, and restore workflows.
 */
@Getter
@Setter
@Entity
@Table(name = "recovery_versions")
public class RecoveryVersion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recovery_record_id")
    private RecoveryRecord recoveryRecord;

    @Column(nullable = false, length = 120)
    private String tenantKey;

    @Column(nullable = false, length = 120)
    private String entityType;

    @Column(nullable = false, length = 120)
    private String entityId;

    @Column(nullable = false)
    private Integer versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RecoveryActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RecoveryLifecycleState lifecycleStateAfter;

    @Column(nullable = false)
    private Instant capturedAt;

    @Column(length = 255)
    private String actor;

    @Column(length = 1000)
    private String reason;

    @Column(nullable = false)
    private boolean backupSnapshot;

    @Column(nullable = false)
    private boolean anonymized;

    private Instant restoredFromAt;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String metadataJson;
}

package com.noura.platform.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "data_exchange_jobs")
public class DataExchangeJob extends AuditedEntity {

    @Column(name = "job_type", nullable = false, length = 40)
    private String jobType;

    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType;

    @Column(name = "file_format", nullable = false, length = 20)
    private String fileFormat;

    @Column(name = "storage_path", length = 1000)
    private String storagePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private IamUser requestedBy;

    @Column(name = "job_status", nullable = false, length = 40)
    private String jobStatus = "PENDING";

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
}

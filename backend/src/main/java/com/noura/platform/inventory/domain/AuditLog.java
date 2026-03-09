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
@Table(name = "audit_logs")
public class AuditLog extends BaseUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private IamUser actorUser;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "action_code", nullable = false, length = 100)
    private String actionCode;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 36)
    private String entityId;

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(name = "before_state_json", columnDefinition = "json")
    private String beforeStateJson;

    @Column(name = "after_state_json", columnDefinition = "json")
    private String afterStateJson;

    @Column(name = "metadata_json", columnDefinition = "json")
    private String metadataJson;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(name = "event_hash", nullable = false, length = 64, unique = true)
    private String eventHash;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}

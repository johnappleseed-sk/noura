package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.MerchantContractActionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable audit trail for contract actions.
 */
@Getter
@Setter
@Entity
@Table(name = "merchant_contract_actions")
public class MerchantContractAction extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id")
    private MerchantContract contract;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MerchantContractActionType action;

    @Column(name = "actor_email")
    private String actorEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private UserAccount actorUser;

    @Column(length = 1000)
    private String note;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadata = new LinkedHashMap<>();

    @Column(name = "correlation_id", length = 120)
    private String correlationId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();
}


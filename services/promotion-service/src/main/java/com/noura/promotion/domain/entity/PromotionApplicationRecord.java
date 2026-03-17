package com.noura.promotion.domain.entity;

import com.noura.promotion.domain.enums.PromotionApplicableEntityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Scope constraint linking one promotion to applicable entity identifiers.
 */
@Getter
@Setter
@Entity
@Table(name = "promotion_applications")
public class PromotionApplicationRecord extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private PromotionRecord promotion;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicable_entity_type", nullable = false, length = 40)
    private PromotionApplicableEntityType applicableEntityType;

    @Column(name = "applicable_entity_id", nullable = false)
    private UUID applicableEntityId;
}

package com.noura.platform.domain.entity;

import com.noura.platform.domain.enums.PromotionApplicableEntityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "promotion_applications")
public class PromotionApplication extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicable_entity_type", nullable = false)
    private PromotionApplicableEntityType applicableEntityType;

    @Column(name = "applicable_entity_id", nullable = false)
    private UUID applicableEntityId;
}

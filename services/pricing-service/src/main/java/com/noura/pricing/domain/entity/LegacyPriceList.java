package com.noura.pricing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Lightweight legacy price-list metadata retained for admin compatibility screens.
 */
@Getter
@Setter
@Entity
@Table(name = "legacy_price_lists")
public class LegacyPriceList extends AuditableEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "customer_group_id")
    private UUID customerGroupId;

    @Column(name = "channel_id")
    private UUID channelId;
}

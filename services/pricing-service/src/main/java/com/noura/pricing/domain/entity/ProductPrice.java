package com.noura.pricing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Product price record scoped by optional store/channel and active window.
 *
 * <p>Each record stores the canonical base sell price and optional compare-at price.
 * Effective price is resolved by the service layer according to scope precedence.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pricing_product_prices")
public class ProductPrice extends AuditableEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "base_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal basePrice;

    @Column(name = "compare_at_price", precision = 18, scale = 4)
    private BigDecimal compareAtPrice;

    @Column(name = "channel_code", length = 80)
    private String channelCode;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "active", nullable = false)
    private boolean active;
}


package com.noura.platform.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_levels")
public class StockLevel extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bin_id")
    private WarehouseBin bin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private BatchLot batch;

    @Column(name = "quantity_on_hand", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(name = "quantity_reserved", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityReserved = BigDecimal.ZERO;

    @Column(name = "quantity_available", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityAvailable = BigDecimal.ZERO;

    @Column(name = "quantity_damaged", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityDamaged = BigDecimal.ZERO;

    @Column(name = "last_movement_at")
    private Instant lastMovementAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}

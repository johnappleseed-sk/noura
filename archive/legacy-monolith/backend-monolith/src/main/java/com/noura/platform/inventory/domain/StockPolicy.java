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

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_policies")
public class StockPolicy extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @Column(name = "low_stock_threshold", nullable = false, precision = 18, scale = 4)
    private BigDecimal lowStockThreshold = BigDecimal.ZERO;

    @Column(name = "reorder_point", nullable = false, precision = 18, scale = 4)
    private BigDecimal reorderPoint = BigDecimal.ZERO;

    @Column(name = "reorder_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal reorderQuantity = BigDecimal.ZERO;

    @Column(name = "max_stock_level", precision = 18, scale = 4)
    private BigDecimal maxStockLevel;

    @Column(name = "allow_backorder", nullable = false)
    private boolean allowBackorder;
}

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
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reorder_alerts")
public class ReorderAlert extends CreatedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bin_id")
    private WarehouseBin bin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_policy_id")
    private StockPolicy stockPolicy;

    @Column(name = "current_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal currentQuantity;

    @Column(name = "threshold_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal thresholdQuantity;

    @Column(name = "alert_status", nullable = false, length = 40)
    private String alertStatus = "OPEN";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by")
    private IamUser acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}

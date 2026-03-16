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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "serial_numbers")
public class SerialNumber extends SoftDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private BatchLot batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bin_id")
    private WarehouseBin bin;

    @Column(name = "serial_number", nullable = false, length = 180, unique = true)
    private String serialNumber;

    @Column(name = "serial_status", nullable = false, length = 40)
    private String serialStatus = "IN_STOCK";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_movement_line_id")
    private StockMovementLine lastMovementLine;
}

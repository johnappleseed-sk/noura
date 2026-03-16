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
@Table(name = "warehouse_bins")
public class WarehouseBin extends SoftDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "bin_code", nullable = false, length = 100)
    private String binCode;

    @Column(name = "zone_code", length = 80)
    private String zoneCode;

    @Column(name = "aisle_code", length = 80)
    private String aisleCode;

    @Column(name = "shelf_code", length = 80)
    private String shelfCode;

    @Column(name = "bin_type", nullable = false, length = 60)
    private String binType = "STANDARD";

    @Column(name = "barcode_value", length = 255)
    private String barcodeValue;

    @Column(name = "qr_code_value", length = 255)
    private String qrCodeValue;

    @Column(name = "pick_sequence", nullable = false)
    private int pickSequence;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}

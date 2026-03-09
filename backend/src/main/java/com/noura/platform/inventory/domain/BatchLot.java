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

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "batch_lots")
public class BatchLot extends SoftDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "lot_number", nullable = false, length = 120)
    private String lotNumber;

    @Column(name = "supplier_batch_ref", length = 120)
    private String supplierBatchRef;

    @Column(name = "manufactured_at")
    private LocalDate manufacturedAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "status", nullable = false, length = 40)
    private String status = "ACTIVE";

    @Column(name = "notes", length = 500)
    private String notes;
}

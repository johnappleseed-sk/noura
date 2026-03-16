package com.noura.inventory.domain.entity;

import com.noura.inventory.domain.enums.StockMovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable audit record describing a stock mutation.
 *
 * <p>Each entry captures before/after snapshots for on-hand, reserved, and available
 * quantities to support traceability and governance.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventory_stock_movements")
public class InventoryStockMovement extends AuditableEntity {

    @Column(name = "stock_level_id", nullable = false)
    private UUID stockLevelId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 40)
    private StockMovementType movementType;

    @Column(name = "quantity_delta", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityDelta;

    @Column(name = "quantity_on_hand_before", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityOnHandBefore;

    @Column(name = "quantity_on_hand_after", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityOnHandAfter;

    @Column(name = "quantity_reserved_before", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityReservedBefore;

    @Column(name = "quantity_reserved_after", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityReservedAfter;

    @Column(name = "quantity_available_before", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityAvailableBefore;

    @Column(name = "quantity_available_after", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityAvailableAfter;

    @Column(name = "reason_code", length = 80)
    private String reasonCode;

    @Column(name = "reference_type", length = 80)
    private String referenceType;

    @Column(name = "reference_id", length = 120)
    private String referenceId;

    @Column(name = "notes", length = 1000)
    private String notes;
}

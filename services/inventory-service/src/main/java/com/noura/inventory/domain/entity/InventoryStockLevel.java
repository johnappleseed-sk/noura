package com.noura.inventory.domain.entity;

import com.noura.inventory.domain.enums.StockStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Persistent stock state for a product at a specific inventory location.
 *
 * <p>Uniqueness is enforced by {@code (product_id, warehouse_id)} to ensure a single
 * canonical balance per product/location pair in this first extraction slice.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "inventory_stock_levels",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_inventory_stock_levels_product_warehouse",
                columnNames = {"product_id", "warehouse_id"}
        )
)
public class InventoryStockLevel extends AuditableEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_sku", length = 100)
    private String productSku;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(name = "warehouse_code", length = 100)
    private String warehouseCode;

    @Column(name = "warehouse_name", length = 255)
    private String warehouseName;

    @Column(name = "bin_id")
    private UUID binId;

    @Column(name = "bin_code", length = 100)
    private String binCode;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "lot_number", length = 120)
    private String lotNumber;

    @Column(name = "quantity_on_hand", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(name = "quantity_reserved", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityReserved = BigDecimal.ZERO;

    @Column(name = "quantity_available", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityAvailable = BigDecimal.ZERO;

    @Column(name = "quantity_damaged", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityDamaged = BigDecimal.ZERO;

    @Column(name = "low_stock_threshold", nullable = false, precision = 18, scale = 4)
    private BigDecimal lowStockThreshold = BigDecimal.valueOf(5);

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_status", nullable = false, length = 40)
    private StockStatus stockStatus = StockStatus.OUT_OF_STOCK;

    @Column(name = "last_movement_at")
    private Instant lastMovementAt;

    /**
     * Recalculates derived stock values and status.
     *
     * <p>Business rule: available stock cannot be negative even if reserved exceeds on-hand,
     * so the value is clamped to zero for safe API consumption.</p>
     */
    public void recalculateAvailability() {
        quantityOnHand = safe(quantityOnHand);
        quantityReserved = safe(quantityReserved);
        lowStockThreshold = safe(lowStockThreshold);
        quantityDamaged = safe(quantityDamaged);

        // Prevent negative availability exposure to upstream callers.
        BigDecimal available = quantityOnHand.subtract(quantityReserved);
        if (available.signum() < 0) {
            available = BigDecimal.ZERO;
        }
        this.quantityAvailable = available;
        this.stockStatus = resolveStockStatus(available, lowStockThreshold);
    }

    /**
     * Resolves stock status from current available quantity and configured threshold.
     *
     * @param available calculated available quantity
     * @param threshold low-stock threshold
     * @return derived stock status
     */
    private StockStatus resolveStockStatus(BigDecimal available, BigDecimal threshold) {
        if (available.signum() <= 0) {
            return StockStatus.OUT_OF_STOCK;
        }
        if (available.compareTo(threshold) <= 0) {
            return StockStatus.LOW_STOCK;
        }
        return StockStatus.IN_STOCK;
    }

    /**
     * Normalizes nullable quantities to zero.
     *
     * @param value quantity value
     * @return value when present, otherwise {@link BigDecimal#ZERO}
     */
    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

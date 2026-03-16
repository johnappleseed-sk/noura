package com.noura.platform.commerce.multistore.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Store-specific inventory levels.
 * Allows tracking inventory per store location.
 */
@Entity
@Table(name = "store_inventory",
        uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "product_id"}))
public class StoreInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    // Quantity on hand at this location
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    // Quantity reserved (in carts, pending orders)
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantityReserved = BigDecimal.ZERO;

    // Quantity on order (purchase orders)
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantityOnOrder = BigDecimal.ZERO;

    // Reorder point for this store
    @Column(precision = 15, scale = 4)
    private BigDecimal reorderPoint;

    // Store-specific location/bin
    @Column(length = 100)
    private String binLocation;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // === Computed ===

    public BigDecimal getQuantityAvailable() {
        return quantityOnHand.subtract(quantityReserved);
    }

    // === Getters and Setters ===

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public BigDecimal getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(BigDecimal quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public BigDecimal getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(BigDecimal quantityReserved) {
        this.quantityReserved = quantityReserved;
    }

    public BigDecimal getQuantityOnOrder() {
        return quantityOnOrder;
    }

    public void setQuantityOnOrder(BigDecimal quantityOnOrder) {
        this.quantityOnOrder = quantityOnOrder;
    }

    public BigDecimal getReorderPoint() {
        return reorderPoint;
    }

    public void setReorderPoint(BigDecimal reorderPoint) {
        this.reorderPoint = reorderPoint;
    }

    public String getBinLocation() {
        return binLocation;
    }

    public void setBinLocation(String binLocation) {
        this.binLocation = binLocation;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

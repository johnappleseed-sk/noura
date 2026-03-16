package com.noura.inventory.domain.enums;

/**
 * Derived availability classification for a stock level.
 */
public enum StockStatus {
    /** Available quantity is higher than configured low-stock threshold. */
    IN_STOCK,
    /** Available quantity is positive but at or below low-stock threshold. */
    LOW_STOCK,
    /** No available quantity remains. */
    OUT_OF_STOCK
}

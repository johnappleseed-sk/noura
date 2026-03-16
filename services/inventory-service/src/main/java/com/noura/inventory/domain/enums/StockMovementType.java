package com.noura.inventory.domain.enums;

/**
 * Supported stock mutation types persisted in movement history.
 */
public enum StockMovementType {
    /** Manual or system quantity correction (positive or negative delta). */
    ADJUSTMENT,
    /** Reservation of available stock for a downstream flow (for example checkout). */
    RESERVE,
    /** Release of previously reserved quantity back to available stock. */
    RELEASE,
    /** Final stock deduction (consumption/fulfillment). */
    DEDUCT
}

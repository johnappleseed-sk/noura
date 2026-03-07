package com.noura.platform.commerce.multistore.domain;

/**
 * Types of stores supported.
 */
public enum StoreType {
    /** Physical retail location */
    RETAIL,

    /** Warehouse (no customer-facing) */
    WAREHOUSE,

    /** Pop-up shop */
    POPUP,

    /** Kiosk */
    KIOSK,

    /** Franchise location */
    FRANCHISE,

    /** Online-only fulfillment center */
    FULFILLMENT_CENTER
}

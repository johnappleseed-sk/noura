CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS inventory_stock_levels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    product_sku VARCHAR(100) NULL,
    product_name VARCHAR(255) NULL,
    warehouse_id UUID NOT NULL,
    warehouse_code VARCHAR(100) NULL,
    warehouse_name VARCHAR(255) NULL,
    bin_id UUID NULL,
    bin_code VARCHAR(100) NULL,
    batch_id UUID NULL,
    lot_number VARCHAR(120) NULL,
    quantity_on_hand NUMERIC(18, 4) NOT NULL DEFAULT 0,
    quantity_reserved NUMERIC(18, 4) NOT NULL DEFAULT 0,
    quantity_available NUMERIC(18, 4) NOT NULL DEFAULT 0,
    quantity_damaged NUMERIC(18, 4) NOT NULL DEFAULT 0,
    low_stock_threshold NUMERIC(18, 4) NOT NULL DEFAULT 5,
    stock_status VARCHAR(40) NOT NULL DEFAULT 'OUT_OF_STOCK',
    last_movement_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120) NULL,
    updated_by VARCHAR(120) NULL,
    CONSTRAINT uk_inventory_stock_levels_product_warehouse UNIQUE (product_id, warehouse_id),
    CONSTRAINT chk_inventory_stock_levels_non_negative CHECK (
        quantity_on_hand >= 0
        AND quantity_reserved >= 0
        AND quantity_available >= 0
        AND quantity_damaged >= 0
        AND low_stock_threshold >= 0
    )
);

CREATE INDEX IF NOT EXISTS idx_inventory_stock_levels_product_id
    ON inventory_stock_levels (product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_stock_levels_warehouse_id
    ON inventory_stock_levels (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_stock_levels_status
    ON inventory_stock_levels (stock_status);
CREATE INDEX IF NOT EXISTS idx_inventory_stock_levels_low_stock
    ON inventory_stock_levels (stock_status, quantity_available, low_stock_threshold);

CREATE TABLE IF NOT EXISTS inventory_stock_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stock_level_id UUID NOT NULL,
    product_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    movement_type VARCHAR(40) NOT NULL,
    quantity_delta NUMERIC(18, 4) NOT NULL,
    quantity_on_hand_before NUMERIC(18, 4) NOT NULL,
    quantity_on_hand_after NUMERIC(18, 4) NOT NULL,
    quantity_reserved_before NUMERIC(18, 4) NOT NULL,
    quantity_reserved_after NUMERIC(18, 4) NOT NULL,
    quantity_available_before NUMERIC(18, 4) NOT NULL,
    quantity_available_after NUMERIC(18, 4) NOT NULL,
    reason_code VARCHAR(80) NULL,
    reference_type VARCHAR(80) NULL,
    reference_id VARCHAR(120) NULL,
    notes VARCHAR(1000) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120) NULL,
    CONSTRAINT fk_inventory_stock_movements_stock_level
        FOREIGN KEY (stock_level_id) REFERENCES inventory_stock_levels (id)
);

CREATE INDEX IF NOT EXISTS idx_inventory_stock_movements_product_warehouse
    ON inventory_stock_movements (product_id, warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_stock_movements_type_created_at
    ON inventory_stock_movements (movement_type, created_at DESC);

ALTER TABLE promotions
    ADD COLUMN IF NOT EXISTS code VARCHAR(255),
    ADD COLUMN IF NOT EXISTS description VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS is_stackable BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS usage_limit_total INTEGER,
    ADD COLUMN IF NOT EXISTS usage_limit_per_customer INTEGER,
    ADD COLUMN IF NOT EXISTS usage_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS customer_segment VARCHAR(120),
    ADD COLUMN IF NOT EXISTS is_archived BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_promotions_code ON promotions (LOWER(code)) WHERE code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_promotions_active_archived_window ON promotions (is_active, is_archived, start_date, end_date);

CREATE TABLE IF NOT EXISTS analytics_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    session_id VARCHAR(120),
    customer_ref VARCHAR(120),
    product_id VARCHAR(120),
    order_id VARCHAR(120),
    promotion_code VARCHAR(120),
    store_id VARCHAR(120),
    channel_id VARCHAR(120),
    locale VARCHAR(32),
    page_path VARCHAR(255),
    source VARCHAR(80),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    metadata_json JSON,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_analytics_events_type_occurred ON analytics_events (event_type, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_analytics_events_product ON analytics_events (product_id);
CREATE INDEX IF NOT EXISTS idx_analytics_events_order ON analytics_events (order_id);

CREATE TABLE IF NOT EXISTS inventory_transfers (
    id UUID PRIMARY KEY,
    variant_id UUID NOT NULL,
    from_warehouse_id UUID NOT NULL,
    to_warehouse_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(40) NOT NULL,
    scheduled_for TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    requested_by VARCHAR(120),
    note VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(120),
    CONSTRAINT fk_inventory_transfers_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_transfers_from_warehouse FOREIGN KEY (from_warehouse_id) REFERENCES warehouses (id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_transfers_to_warehouse FOREIGN KEY (to_warehouse_id) REFERENCES warehouses (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_inventory_transfers_status_created ON inventory_transfers (status, created_at DESC);

CREATE TABLE IF NOT EXISTS inventory_restock_schedules (
    id UUID PRIMARY KEY,
    variant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    target_quantity INTEGER NOT NULL,
    status VARCHAR(40) NOT NULL,
    scheduled_for TIMESTAMP WITH TIME ZONE NOT NULL,
    requested_by VARCHAR(120),
    note VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(120),
    CONSTRAINT fk_inventory_restock_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_restock_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_inventory_restock_status_schedule ON inventory_restock_schedules (status, scheduled_for ASC);

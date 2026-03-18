CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(40) NOT NULL,
    customer_ref VARCHAR(180) NOT NULL,
    store_id UUID NULL,
    address_id UUID NULL,
    currency_code VARCHAR(8) NOT NULL,
    subtotal NUMERIC(14, 4) NOT NULL,
    discount_amount NUMERIC(14, 4) NOT NULL DEFAULT 0,
    shipping_amount NUMERIC(14, 4) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(14, 4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(14, 4) NOT NULL,
    payment_reference VARCHAR(255) NULL,
    coupon_code VARCHAR(80) NULL,
    shipping_address_snapshot_json TEXT NULL,
    billing_address_snapshot_json TEXT NULL,
    checkout_snapshot_json TEXT NULL,
    status VARCHAR(32) NOT NULL,
    refund_status VARCHAR(32) NOT NULL DEFAULT 'NONE',
    idempotency_key VARCHAR(128) NULL,
    placed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(180) NULL,
    updated_by VARCHAR(180) NULL,
    CONSTRAINT uk_orders_order_number UNIQUE (order_number),
    CONSTRAINT chk_orders_non_negative_totals CHECK (
        subtotal >= 0
        AND discount_amount >= 0
        AND shipping_amount >= 0
        AND tax_amount >= 0
        AND total_amount >= 0
    )
);

CREATE INDEX IF NOT EXISTS idx_orders_customer_ref_placed_at
    ON orders (customer_ref, placed_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_status_placed_at
    ON orders (status, placed_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_refund_status_placed_at
    ON orders (refund_status, placed_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_store_id
    ON orders (store_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_customer_ref_idempotency_key
    ON orders (customer_ref, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    line_number INTEGER NOT NULL,
    product_id UUID NOT NULL,
    variant_id UUID NULL,
    sku VARCHAR(120) NULL,
    product_name VARCHAR(255) NOT NULL,
    variant_name VARCHAR(160) NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(14, 4) NOT NULL,
    line_total NUMERIC(14, 4) NOT NULL,
    item_snapshot_json TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(180) NULL,
    updated_by VARCHAR(180) NULL,
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT uk_order_items_line_number UNIQUE (order_id, line_number),
    CONSTRAINT chk_order_items_positive_values CHECK (
        quantity > 0
        AND unit_price >= 0
        AND line_total >= 0
    )
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id
    ON order_items (order_id);

CREATE INDEX IF NOT EXISTS idx_order_items_product_id
    ON order_items (product_id);

CREATE TABLE IF NOT EXISTS order_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    refund_status VARCHAR(32) NOT NULL,
    reason VARCHAR(255) NULL,
    note VARCHAR(600) NULL,
    changed_by VARCHAR(180) NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(180) NULL,
    updated_by VARCHAR(180) NULL,
    CONSTRAINT fk_order_status_history_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_status_history_order_id_changed_at
    ON order_status_history (order_id, changed_at ASC);


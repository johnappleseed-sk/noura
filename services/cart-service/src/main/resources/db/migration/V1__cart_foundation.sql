CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_type VARCHAR(20) NOT NULL,
    customer_id VARCHAR(180) NULL,
    guest_token VARCHAR(180) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    store_id UUID NULL,
    address_id UUID NULL,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    subtotal NUMERIC(18, 4) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(18, 4) NOT NULL DEFAULT 0,
    shipping_amount NUMERIC(18, 4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(18, 4) NOT NULL DEFAULT 0,
    coupon_code VARCHAR(120) NULL,
    merged_into_cart_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120) NULL,
    updated_by VARCHAR(120) NULL,
    CONSTRAINT fk_carts_merged_into FOREIGN KEY (merged_into_cart_id) REFERENCES carts (id),
    CONSTRAINT chk_carts_owner_type CHECK (owner_type IN ('CUSTOMER', 'GUEST')),
    CONSTRAINT chk_carts_status CHECK (status IN ('ACTIVE', 'MERGED', 'CHECKED_OUT')),
    CONSTRAINT chk_carts_owner_fields CHECK (
        (owner_type = 'CUSTOMER' AND customer_id IS NOT NULL)
        OR (owner_type = 'GUEST' AND guest_token IS NOT NULL)
    ),
    CONSTRAINT chk_carts_non_negative_totals CHECK (
        subtotal >= 0 AND discount_amount >= 0 AND shipping_amount >= 0 AND total_amount >= 0
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_carts_customer_active
    ON carts (customer_id)
    WHERE owner_type = 'CUSTOMER' AND status = 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS uk_carts_guest_active
    ON carts (guest_token)
    WHERE owner_type = 'GUEST' AND status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_carts_status ON carts (status);
CREATE INDEX IF NOT EXISTS idx_carts_store_id ON carts (store_id);
CREATE INDEX IF NOT EXISTS idx_carts_updated_at ON carts (updated_at DESC);

CREATE TABLE IF NOT EXISTS cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id UUID NOT NULL,
    product_id UUID NOT NULL,
    variant_id UUID NULL,
    store_id UUID NULL,
    product_code_snapshot VARCHAR(120) NULL,
    product_name_snapshot VARCHAR(255) NOT NULL,
    sku_snapshot VARCHAR(120) NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(18, 4) NOT NULL DEFAULT 0,
    line_total NUMERIC(18, 4) NOT NULL DEFAULT 0,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'USD',
    validation_status VARCHAR(40) NOT NULL DEFAULT 'VALID',
    validation_message VARCHAR(500) NULL,
    available_quantity NUMERIC(18, 4) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120) NULL,
    updated_by VARCHAR(120) NULL,
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT chk_cart_items_positive_qty CHECK (quantity > 0),
    CONSTRAINT chk_cart_items_non_negative_money CHECK (unit_price >= 0 AND line_total >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_cart_items_dedup
    ON cart_items (
        cart_id,
        product_id,
        COALESCE(variant_id, '00000000-0000-0000-0000-000000000000'::UUID),
        COALESCE(store_id, '00000000-0000-0000-0000-000000000000'::UUID)
    );

CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id_created_at
    ON cart_items (cart_id, created_at);
CREATE INDEX IF NOT EXISTS idx_cart_items_product_id
    ON cart_items (product_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_store_id
    ON cart_items (store_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_validation_status
    ON cart_items (validation_status);

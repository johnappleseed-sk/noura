ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS parent_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_categories_parent'
    ) THEN
        ALTER TABLE categories
            ADD CONSTRAINT fk_categories_parent
            FOREIGN KEY (parent_id) REFERENCES categories (id) ON DELETE SET NULL;
    END IF;
END $$;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS attributes JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE product_variants
    ADD COLUMN IF NOT EXISTS attributes JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS price_override NUMERIC(14, 2),
    ADD COLUMN IF NOT EXISTS stock INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE product_variants
    ALTER COLUMN color DROP NOT NULL,
    ALTER COLUMN size DROP NOT NULL;

ALTER TABLE product_media
    ADD COLUMN IF NOT EXISTS is_primary BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS attributes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(40) NOT NULL,
    possible_values JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS attribute_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS attribute_set_attributes (
    attribute_set_id UUID NOT NULL,
    attribute_id UUID NOT NULL,
    PRIMARY KEY (attribute_set_id, attribute_id),
    CONSTRAINT fk_attribute_set_attributes_set
        FOREIGN KEY (attribute_set_id) REFERENCES attribute_sets (id) ON DELETE CASCADE,
    CONSTRAINT fk_attribute_set_attributes_attribute
        FOREIGN KEY (attribute_id) REFERENCES attributes (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS warehouses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    location VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    reorder_point INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_inventory_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id) ON DELETE CASCADE,
    CONSTRAINT uk_inventory_variant_warehouse UNIQUE (variant_id, warehouse_id)
);

CREATE TABLE IF NOT EXISTS inventory_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    order_id UUID,
    quantity INTEGER NOT NULL,
    status VARCHAR(40) NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_inventory_reservation_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_reservation_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS inventory_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    change_quantity INTEGER NOT NULL,
    type VARCHAR(40) NOT NULL,
    order_id UUID,
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_inventory_transaction_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_transaction_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS price_lists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(40) NOT NULL,
    customer_group_id UUID,
    channel_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS prices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variant_id UUID NOT NULL,
    price_list_id UUID NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    start_date TIMESTAMPTZ,
    end_date TIMESTAMPTZ,
    priority INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_prices_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id) ON DELETE CASCADE,
    CONSTRAINT fk_prices_price_list FOREIGN KEY (price_list_id) REFERENCES price_lists (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS promotions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(40) NOT NULL,
    coupon_code VARCHAR(255),
    conditions_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    start_date TIMESTAMPTZ,
    end_date TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS promotion_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promotion_id UUID NOT NULL,
    applicable_entity_type VARCHAR(40) NOT NULL,
    applicable_entity_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_promotion_applications_promotion FOREIGN KEY (promotion_id) REFERENCES promotions (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_categories_parent ON categories (parent_id);
CREATE INDEX IF NOT EXISTS idx_products_active ON products (active);
CREATE INDEX IF NOT EXISTS idx_products_category_created ON products (category_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_products_attributes_gin ON products USING gin (attributes);
CREATE INDEX IF NOT EXISTS idx_product_variants_attributes_gin ON product_variants USING gin (attributes);
CREATE INDEX IF NOT EXISTS idx_product_variants_sku ON product_variants (sku);
CREATE INDEX IF NOT EXISTS idx_inventory_variant ON inventory (variant_id);
CREATE INDEX IF NOT EXISTS idx_inventory_warehouse ON inventory (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_variant_created ON inventory_transactions (variant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_prices_variant_time ON prices (variant_id, start_date, end_date, priority);
CREATE INDEX IF NOT EXISTS idx_promotions_active_window ON promotions (is_active, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_promotion_applications_target ON promotion_applications (applicable_entity_type, applicable_entity_id);


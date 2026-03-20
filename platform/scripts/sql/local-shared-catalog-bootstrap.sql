CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS brands (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_brands_code
    ON brands (code);

CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description TEXT NULL,
    classification_code VARCHAR(64) NULL,
    level INTEGER NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    manager_id UUID NULL,
    parent_id UUID NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_categories_code
    ON categories (code);

CREATE TABLE IF NOT EXISTS stores (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY,
    product_code VARCHAR(120) NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    category_id UUID NULL,
    brand_id UUID NULL,
    base_price NUMERIC(18, 4) NOT NULL DEFAULT 0,
    attributes JSONB NOT NULL DEFAULT '{}'::JSONB,
    status VARCHAR(40) NULL,
    approval_status VARCHAR(40) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    allow_backorder BOOLEAN NOT NULL DEFAULT FALSE,
    flash_sale BOOLEAN NOT NULL DEFAULT FALSE,
    trending BOOLEAN NOT NULL DEFAULT FALSE,
    best_seller BOOLEAN NOT NULL DEFAULT FALSE,
    average_rating DOUBLE PRECISION NOT NULL DEFAULT 0,
    review_count INTEGER NOT NULL DEFAULT 0,
    popularity_score INTEGER NOT NULL DEFAULT 0,
    short_description TEXT NULL,
    long_description TEXT NULL,
    target_audience VARCHAR(255) NULL,
    barcode VARCHAR(120) NULL,
    qr_code VARCHAR(120) NULL,
    seo_title VARCHAR(255) NULL,
    seo_description TEXT NULL,
    seo_slug VARCHAR(255) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_products_product_code
    ON products (product_code);

CREATE TABLE IF NOT EXISTS product_variants (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    color VARCHAR(80) NULL,
    size VARCHAR(80) NULL,
    sku VARCHAR(120) NULL,
    variant_name VARCHAR(255) NULL,
    barcode VARCHAR(120) NULL,
    attributes JSONB NOT NULL DEFAULT '{}'::JSONB,
    price_override NUMERIC(18, 4) NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_product_variants_product_id
    ON product_variants (product_id);

CREATE TABLE IF NOT EXISTS product_media (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    media_type VARCHAR(40) NOT NULL,
    url TEXT NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_product_media_product_id
    ON product_media (product_id);

CREATE TABLE IF NOT EXISTS product_inventory (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    store_id UUID NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    store_price NUMERIC(18, 4) NULL,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    local_name VARCHAR(255) NULL
);

CREATE INDEX IF NOT EXISTS idx_product_inventory_product_store
    ON product_inventory (product_id, store_id);

INSERT INTO brands (id, code, name, slug, active)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'NOURA-LABS',
    'Noura Labs',
    'noura-labs',
    TRUE
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO categories (id, code, name, slug, description, classification_code, level, active)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    'SKINCARE',
    'Skincare',
    'skincare',
    'Local demo category for the NOURA storefront bootstrap.',
    'BEAUTY',
    1,
    TRUE
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO stores (id, name)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    'Noura Phnom Penh Flagship'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO products (
    id,
    product_code,
    name,
    slug,
    category_id,
    brand_id,
    base_price,
    attributes,
    status,
    approval_status,
    active,
    allow_backorder,
    flash_sale,
    trending,
    best_seller,
    average_rating,
    review_count,
    popularity_score,
    short_description,
    long_description,
    target_audience,
    seo_title,
    seo_description,
    seo_slug,
    created_at,
    updated_at
) VALUES (
    '44444444-4444-4444-4444-444444444444',
    'NOURA-SERUM-001',
    'Hydrating Glow Serum',
    'hydrating-glow-serum',
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    29.9000,
    '{"benefit":"hydration","texture":"serum","size":"30ml"}'::JSONB,
    'PUBLISHED',
    'APPROVED',
    TRUE,
    FALSE,
    TRUE,
    TRUE,
    TRUE,
    4.7,
    18,
    95,
    'Barrier-friendly serum with niacinamide and hyaluronic acid.',
    'A pragmatic demo product used to verify catalog, pricing, cart, checkout, and search flows in local development.',
    'all',
    'Hydrating Glow Serum | NOURA',
    'Local bootstrap demo product for NOURA development.',
    'hydrating-glow-serum',
    NOW(),
    NOW()
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO product_variants (
    id,
    product_id,
    color,
    size,
    sku,
    variant_name,
    barcode,
    attributes,
    price_override,
    stock,
    active
) VALUES (
    '55555555-5555-5555-5555-555555555555',
    '44444444-4444-4444-4444-444444444444',
    'Clear',
    '30ml',
    'NOURA-SERUM-001-30ML',
    'Hydrating Glow Serum / 30ml',
    '890000000001',
    '{"pack":"single"}'::JSONB,
    29.9000,
    24,
    TRUE
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO product_media (
    id,
    product_id,
    media_type,
    url,
    sort_order,
    is_primary
) VALUES (
    '66666666-6666-6666-6666-666666666666',
    '44444444-4444-4444-4444-444444444444',
    'IMAGE',
    'https://placehold.co/1200x1200/png?text=NOURA+Hydrating+Glow+Serum',
    0,
    TRUE
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO product_inventory (
    id,
    product_id,
    store_id,
    stock,
    store_price,
    published,
    visible,
    local_name
) VALUES (
    '77777777-7777-7777-7777-777777777777',
    '44444444-4444-4444-4444-444444444444',
    '33333333-3333-3333-3333-333333333333',
    24,
    29.9000,
    TRUE,
    TRUE,
    'Hydrating Glow Serum'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO inventory_stock_levels (
    id,
    product_id,
    product_sku,
    product_name,
    warehouse_id,
    warehouse_code,
    warehouse_name,
    quantity_on_hand,
    quantity_reserved,
    quantity_available,
    quantity_damaged,
    low_stock_threshold,
    stock_status,
    last_movement_at,
    created_by,
    updated_by
)
SELECT
    '88888888-8888-8888-8888-888888888888',
    '44444444-4444-4444-4444-444444444444',
    'NOURA-SERUM-001-30ML',
    'Hydrating Glow Serum',
    '33333333-3333-3333-3333-333333333333',
    'flagship-store',
    'Noura Phnom Penh Flagship',
    24.0000,
    0.0000,
    24.0000,
    0.0000,
    5.0000,
    'IN_STOCK',
    NOW(),
    'local-bootstrap',
    'local-bootstrap'
WHERE NOT EXISTS (
    SELECT 1
    FROM inventory_stock_levels
    WHERE product_id = '44444444-4444-4444-4444-444444444444'
      AND warehouse_id = '33333333-3333-3333-3333-333333333333'
);

INSERT INTO pricing_product_prices (
    id,
    product_id,
    currency_code,
    base_price,
    compare_at_price,
    channel_code,
    store_id,
    starts_at,
    ends_at,
    priority,
    active,
    created_by,
    updated_by
)
SELECT
    '99999999-9999-9999-9999-999999999999',
    '44444444-4444-4444-4444-444444444444',
    'USD',
    29.9000,
    34.9000,
    'web',
    NULL,
    NULL,
    NULL,
    10,
    TRUE,
    'local-bootstrap',
    'local-bootstrap'
WHERE NOT EXISTS (
    SELECT 1
    FROM pricing_product_prices
    WHERE product_id = '44444444-4444-4444-4444-444444444444'
      AND currency_code = 'USD'
      AND channel_code = 'web'
      AND store_id IS NULL
);

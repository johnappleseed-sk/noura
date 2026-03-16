-- Catalog foundation module.
-- Adds canonical master-catalog identity fields for category, brand, product, and variant.

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS slug VARCHAR(255),
    ADD COLUMN IF NOT EXISTS level INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE brands
    ADD COLUMN IF NOT EXISTS code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS slug VARCHAR(255),
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS product_code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS slug VARCHAR(255),
    ADD COLUMN IF NOT EXISTS approval_status VARCHAR(40) NOT NULL DEFAULT 'PENDING';

ALTER TABLE product_variants
    ADD COLUMN IF NOT EXISTS variant_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS barcode VARCHAR(64);

UPDATE categories
SET code = COALESCE(
    NULLIF(BTRIM(UPPER(REGEXP_REPLACE(COALESCE(code, ''), '[^A-Z0-9]+', '-', 'g'))), ''),
    NULLIF(BTRIM(UPPER(REGEXP_REPLACE(COALESCE(classification_code, ''), '[^A-Z0-9]+', '-', 'g'))), ''),
    'CAT-' || UPPER(SUBSTRING(REPLACE(id::text, '-', '') FROM 1 FOR 8))
);

WITH RECURSIVE category_levels AS (
    SELECT id, parent_id, 0 AS lvl
    FROM categories
    WHERE parent_id IS NULL

    UNION ALL

    SELECT child.id, child.parent_id, parent.lvl + 1
    FROM categories child
    JOIN category_levels parent ON child.parent_id = parent.id
)
UPDATE categories c
SET level = COALESCE(cl.lvl, 0)
FROM category_levels cl
WHERE c.id = cl.id;

UPDATE categories
SET level = 0
WHERE level IS NULL;

WITH base AS (
    SELECT
        id,
        LEFT(
            COALESCE(
                NULLIF(
                    REGEXP_REPLACE(
                        REGEXP_REPLACE(LOWER(BTRIM(COALESCE(slug, name, 'category'))), '[^a-z0-9]+', '-', 'g'),
                        '(^-|-$)',
                        '',
                        'g'
                    ),
                    ''
                ),
                'category-' || SUBSTRING(REPLACE(id::text, '-', '') FROM 1 FOR 8)
            ),
            240
        ) AS base_slug
    FROM categories
),
ranked AS (
    SELECT
        id,
        base_slug,
        ROW_NUMBER() OVER (PARTITION BY base_slug ORDER BY id) AS seq
    FROM base
)
UPDATE categories c
SET slug = CASE
               WHEN r.seq = 1 THEN r.base_slug
               ELSE LEFT(r.base_slug, 232) || '-' || LPAD(r.seq::text, 3, '0')
           END
FROM ranked r
WHERE c.id = r.id;

UPDATE brands
SET code = COALESCE(
    NULLIF(BTRIM(UPPER(REGEXP_REPLACE(COALESCE(code, ''), '[^A-Z0-9]+', '-', 'g'))), ''),
    'BRD-' || UPPER(SUBSTRING(REPLACE(id::text, '-', '') FROM 1 FOR 8))
);

WITH base AS (
    SELECT
        id,
        LEFT(
            COALESCE(
                NULLIF(
                    REGEXP_REPLACE(
                        REGEXP_REPLACE(LOWER(BTRIM(COALESCE(slug, name, 'brand'))), '[^a-z0-9]+', '-', 'g'),
                        '(^-|-$)',
                        '',
                        'g'
                    ),
                    ''
                ),
                'brand-' || SUBSTRING(REPLACE(id::text, '-', '') FROM 1 FOR 8)
            ),
            240
        ) AS base_slug
    FROM brands
),
ranked AS (
    SELECT
        id,
        base_slug,
        ROW_NUMBER() OVER (PARTITION BY base_slug ORDER BY id) AS seq
    FROM base
)
UPDATE brands b
SET slug = CASE
               WHEN r.seq = 1 THEN r.base_slug
               ELSE LEFT(r.base_slug, 232) || '-' || LPAD(r.seq::text, 3, '0')
           END
FROM ranked r
WHERE b.id = r.id;

UPDATE products
SET product_code = COALESCE(
    NULLIF(BTRIM(UPPER(REGEXP_REPLACE(COALESCE(product_code, ''), '[^A-Z0-9]+', '-', 'g'))), ''),
    'PRD-' || UPPER(SUBSTRING(REPLACE(id::text, '-', '') FROM 1 FOR 10))
);

WITH base AS (
    SELECT
        id,
        LEFT(
            COALESCE(
                NULLIF(
                    REGEXP_REPLACE(
                        REGEXP_REPLACE(LOWER(BTRIM(COALESCE(slug, seo_slug, name, 'product'))), '[^a-z0-9]+', '-', 'g'),
                        '(^-|-$)',
                        '',
                        'g'
                    ),
                    ''
                ),
                'product-' || SUBSTRING(REPLACE(id::text, '-', '') FROM 1 FOR 10)
            ),
            240
        ) AS base_slug
    FROM products
),
ranked AS (
    SELECT
        id,
        base_slug,
        ROW_NUMBER() OVER (PARTITION BY base_slug ORDER BY created_at, id) AS seq
    FROM base
)
UPDATE products p
SET slug = CASE
               WHEN r.seq = 1 THEN r.base_slug
               ELSE LEFT(r.base_slug, 232) || '-' || LPAD(r.seq::text, 3, '0')
           END
FROM ranked r
WHERE p.id = r.id;

UPDATE products
SET approval_status = COALESCE(NULLIF(BTRIM(UPPER(approval_status)), ''), 'PENDING')
WHERE TRUE;

UPDATE product_variants
SET variant_name = COALESCE(
    NULLIF(BTRIM(variant_name), ''),
    NULLIF(BTRIM(CONCAT_WS(' ', color, size)), ''),
    NULLIF(BTRIM(sku), ''),
    'VARIANT-' || UPPER(SUBSTRING(REPLACE(id::text, '-', '') FROM 1 FOR 8))
);

UPDATE product_variants
SET barcode = NULLIF(BTRIM(barcode), '')
WHERE barcode IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_categories_code_ci
    ON categories (LOWER(code))
    WHERE code IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_categories_slug_ci
    ON categories (LOWER(slug))
    WHERE slug IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_brands_code_ci
    ON brands (LOWER(code))
    WHERE code IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_brands_slug_ci
    ON brands (LOWER(slug))
    WHERE slug IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_products_product_code_ci
    ON products (LOWER(product_code))
    WHERE product_code IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_products_slug_ci
    ON products (LOWER(slug))
    WHERE slug IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_categories_active_level
    ON categories (active, level, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_brands_active_created
    ON brands (active, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_products_category_status_created
    ON products (category_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_products_brand_status_created
    ON products (brand_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_product_variants_product_active
    ON product_variants (product_id, active);

CREATE INDEX IF NOT EXISTS idx_product_variants_barcode
    ON product_variants (barcode);

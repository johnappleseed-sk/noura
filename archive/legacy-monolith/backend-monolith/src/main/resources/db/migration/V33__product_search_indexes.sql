CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_products_category_brand_status
    ON products (category_id, brand_id, status);

CREATE INDEX IF NOT EXISTS idx_products_active_status_updated
    ON products (active, status, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_products_name_trgm
    ON products
    USING gin (LOWER(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_products_product_code_trgm
    ON products
    USING gin (LOWER(product_code) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_product_variants_sku_trgm
    ON product_variants
    USING gin (LOWER(sku) gin_trgm_ops);

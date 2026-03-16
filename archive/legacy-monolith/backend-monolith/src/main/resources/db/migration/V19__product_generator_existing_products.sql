ALTER TABLE products
    ADD COLUMN IF NOT EXISTS target_audience VARCHAR(255),
    ADD COLUMN IF NOT EXISTS barcode VARCHAR(32),
    ADD COLUMN IF NOT EXISTS qr_code VARCHAR(1024);

CREATE UNIQUE INDEX IF NOT EXISTS uk_products_barcode
    ON products (barcode)
    WHERE barcode IS NOT NULL;

CREATE TABLE IF NOT EXISTS product_generator_bridge (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    commerce_product_id UUID NOT NULL UNIQUE,
    inventory_product_id VARCHAR(36) NOT NULL UNIQUE,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_pg_bridge_product FOREIGN KEY (commerce_product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS product_generator_mirror_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    commerce_product_id UUID NOT NULL,
    status VARCHAR(40) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 5,
    next_retry_at TIMESTAMPTZ,
    last_error VARCHAR(2000),
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_pg_mirror_job_product FOREIGN KEY (commerce_product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pg_mirror_jobs_status_retry
    ON product_generator_mirror_jobs (status, next_retry_at);

CREATE INDEX IF NOT EXISTS idx_pg_mirror_jobs_product
    ON product_generator_mirror_jobs (commerce_product_id, created_at DESC);

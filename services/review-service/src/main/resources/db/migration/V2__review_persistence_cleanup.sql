UPDATE product_reviews
SET created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, created_at, NOW())
WHERE created_at IS NULL
   OR updated_at IS NULL;

ALTER TABLE product_reviews
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN updated_at SET DEFAULT NOW();

CREATE INDEX IF NOT EXISTS ix_product_reviews_product_created
    ON product_reviews (product_id, created_at DESC);

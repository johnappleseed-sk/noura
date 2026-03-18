UPDATE shipment_records
SET created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, created_at, NOW())
WHERE created_at IS NULL
   OR updated_at IS NULL;

UPDATE merchant_records
SET created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, created_at, NOW())
WHERE created_at IS NULL
   OR updated_at IS NULL;

UPDATE store_records
SET created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, created_at, NOW())
WHERE created_at IS NULL
   OR updated_at IS NULL;

UPDATE service_area_records
SET created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, created_at, NOW())
WHERE created_at IS NULL
   OR updated_at IS NULL;

ALTER TABLE shipment_records
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN updated_at SET DEFAULT NOW();

ALTER TABLE merchant_records
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN updated_at SET DEFAULT NOW();

ALTER TABLE store_records
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN updated_at SET DEFAULT NOW();

ALTER TABLE service_area_records
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN updated_at SET DEFAULT NOW();

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_store_records_merchant'
    ) THEN
        ALTER TABLE store_records
            ADD CONSTRAINT fk_store_records_merchant
                FOREIGN KEY (merchant_id) REFERENCES merchant_records (id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_merchant_records_code_lower
    ON merchant_records (LOWER(merchant_code));

CREATE INDEX IF NOT EXISTS idx_store_records_code_lower
    ON store_records (LOWER(store_code));

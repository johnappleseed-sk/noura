UPDATE promotions
SET created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, created_at, NOW())
WHERE created_at IS NULL
   OR updated_at IS NULL;

UPDATE promotion_applications
SET created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, created_at, NOW())
WHERE created_at IS NULL
   OR updated_at IS NULL;

ALTER TABLE promotions
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN updated_at SET DEFAULT NOW();

ALTER TABLE promotion_applications
    ALTER COLUMN created_at SET DEFAULT NOW(),
    ALTER COLUMN updated_at SET DEFAULT NOW();

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_promotions_window'
    ) THEN
        ALTER TABLE promotions
            ADD CONSTRAINT chk_promotions_window
                CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_promotions_usage_non_negative'
    ) THEN
        ALTER TABLE promotions
            ADD CONSTRAINT chk_promotions_usage_non_negative
                CHECK (
                    usage_count >= 0
                    AND (usage_limit_total IS NULL OR usage_limit_total >= 0)
                    AND (usage_limit_per_customer IS NULL OR usage_limit_per_customer >= 0)
                );
    END IF;
END $$;

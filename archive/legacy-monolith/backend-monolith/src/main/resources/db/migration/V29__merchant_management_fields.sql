-- Merchant management fields and search support.

ALTER TABLE merchants
    ADD COLUMN IF NOT EXISTS merchant_code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS country_code VARCHAR(12),
    ADD COLUMN IF NOT EXISTS contract_start_at DATE,
    ADD COLUMN IF NOT EXISTS contract_end_at DATE,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

UPDATE merchants
SET merchant_code = 'MER-' || LEFT(REPLACE(id::text, '-', ''), 8)
WHERE merchant_code IS NULL OR TRIM(merchant_code) = '';

UPDATE merchants
SET display_name = COALESCE(NULLIF(TRIM(display_name), ''), COALESCE(NULLIF(TRIM(name), ''), 'UNNAMED MERCHANT'))
WHERE display_name IS NULL OR TRIM(display_name) = '';

UPDATE merchants
SET name = COALESCE(NULLIF(TRIM(name), ''), COALESCE(NULLIF(TRIM(display_name), ''), 'UNNAMED MERCHANT'))
WHERE name IS NULL OR TRIM(name) = '';

UPDATE merchants
SET legal_name = COALESCE(NULLIF(TRIM(legal_name), ''), COALESCE(NULLIF(TRIM(name), ''), 'UNNAMED MERCHANT'))
WHERE legal_name IS NULL OR TRIM(legal_name) = '';

UPDATE merchants
SET country_code = NULLIF(TRIM(country_code), '');

CREATE UNIQUE INDEX IF NOT EXISTS uk_merchants_merchant_code_ci
    ON merchants (LOWER(merchant_code));

CREATE INDEX IF NOT EXISTS idx_merchants_display_name
    ON merchants (LOWER(display_name));

CREATE INDEX IF NOT EXISTS idx_merchants_legal_name
    ON merchants (LOWER(legal_name));

CREATE INDEX IF NOT EXISTS idx_merchants_country_code
    ON merchants (country_code);

CREATE INDEX IF NOT EXISTS idx_merchants_contract_window
    ON merchants (contract_start_at, contract_end_at);

ALTER TABLE merchants
    ALTER COLUMN merchant_code SET NOT NULL;

ALTER TABLE merchants
    ALTER COLUMN display_name SET NOT NULL;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS pricing_currencies (
    code VARCHAR(3) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    decimal_places SMALLINT NOT NULL DEFAULT 2,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    default_currency BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120) NULL,
    updated_by VARCHAR(120) NULL,
    CONSTRAINT chk_pricing_currencies_code_upper CHECK (code = UPPER(code))
);

CREATE TABLE IF NOT EXISTS pricing_product_prices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    base_price NUMERIC(18, 4) NOT NULL,
    compare_at_price NUMERIC(18, 4) NULL,
    channel_code VARCHAR(80) NULL,
    store_id UUID NULL,
    starts_at TIMESTAMPTZ NULL,
    ends_at TIMESTAMPTZ NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120) NULL,
    updated_by VARCHAR(120) NULL,
    CONSTRAINT fk_pricing_product_prices_currency
        FOREIGN KEY (currency_code) REFERENCES pricing_currencies (code),
    CONSTRAINT chk_pricing_product_prices_non_negative CHECK (
        base_price >= 0
        AND (compare_at_price IS NULL OR compare_at_price >= 0)
    ),
    CONSTRAINT chk_pricing_product_prices_window CHECK (
        starts_at IS NULL OR ends_at IS NULL OR ends_at >= starts_at
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_pricing_product_prices_natural_key
    ON pricing_product_prices (
        product_id,
        UPPER(currency_code),
        COALESCE(store_id::TEXT, '00000000-0000-0000-0000-000000000000'),
        COALESCE(channel_code, '__GLOBAL__'),
        COALESCE(starts_at, '1970-01-01T00:00:00Z'::TIMESTAMPTZ),
        COALESCE(ends_at, '9999-12-31T23:59:59Z'::TIMESTAMPTZ)
    );

CREATE INDEX IF NOT EXISTS idx_pricing_product_prices_product_currency
    ON pricing_product_prices (product_id, currency_code);
CREATE INDEX IF NOT EXISTS idx_pricing_product_prices_store
    ON pricing_product_prices (store_id);
CREATE INDEX IF NOT EXISTS idx_pricing_product_prices_channel
    ON pricing_product_prices (channel_code);
CREATE INDEX IF NOT EXISTS idx_pricing_product_prices_active_window
    ON pricing_product_prices (active, starts_at, ends_at);
CREATE INDEX IF NOT EXISTS idx_pricing_product_prices_resolution
    ON pricing_product_prices (
        product_id,
        store_id,
        channel_code,
        priority DESC,
        starts_at DESC,
        updated_at DESC
    );

INSERT INTO pricing_currencies (code, name, decimal_places, active, default_currency, created_by, updated_by)
VALUES
    ('USD', 'US Dollar', 2, TRUE, TRUE, 'flyway', 'flyway'),
    ('KHR', 'Cambodian Riel', 2, TRUE, FALSE, 'flyway', 'flyway')
ON CONFLICT (code) DO UPDATE
SET
    name = EXCLUDED.name,
    decimal_places = EXCLUDED.decimal_places,
    active = EXCLUDED.active,
    default_currency = EXCLUDED.default_currency,
    updated_at = NOW(),
    updated_by = 'flyway';


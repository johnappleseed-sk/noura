CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS legacy_price_lists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    type VARCHAR(32) NOT NULL,
    customer_group_id UUID NULL,
    channel_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120) NULL,
    updated_by VARCHAR(120) NULL
);

CREATE INDEX IF NOT EXISTS idx_legacy_price_lists_name
    ON legacy_price_lists (name);

CREATE INDEX IF NOT EXISTS idx_legacy_price_lists_channel_id
    ON legacy_price_lists (channel_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_pricing_currencies_single_default
    ON pricing_currencies (default_currency)
    WHERE default_currency = TRUE;

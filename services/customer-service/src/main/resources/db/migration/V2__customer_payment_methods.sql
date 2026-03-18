CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS customer_payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    method_type VARCHAR(40) NOT NULL,
    provider VARCHAR(80) NOT NULL,
    tokenized_reference VARCHAR(255) NOT NULL,
    default_method BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120) NULL,
    updated_by VARCHAR(120) NULL,
    CONSTRAINT fk_customer_payment_methods_customer
        FOREIGN KEY (customer_id) REFERENCES customer_profiles (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_customer_payment_methods_customer_updated_at
    ON customer_payment_methods (customer_id, updated_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_payment_methods_default_method
    ON customer_payment_methods (customer_id)
    WHERE default_method = TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_payment_methods_tokenized_reference
    ON customer_payment_methods (customer_id, provider, tokenized_reference);

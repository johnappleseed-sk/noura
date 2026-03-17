CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    customer_ref VARCHAR(180) NOT NULL,
    payment_reference VARCHAR(64) NOT NULL,
    method_type VARCHAR(40) NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider_code VARCHAR(64) NOT NULL,
    provider_transaction_id VARCHAR(128) NULL,
    idempotency_key VARCHAR(128) NULL,
    amount NUMERIC(14, 4) NOT NULL,
    currency_code VARCHAR(8) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    authorized_at TIMESTAMPTZ NULL,
    captured_at TIMESTAMPTZ NULL,
    completed_at TIMESTAMPTZ NULL,
    failure_reason VARCHAR(500) NULL,
    metadata_json TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(180) NULL,
    updated_by VARCHAR(180) NULL,
    CONSTRAINT uk_payment_transactions_reference UNIQUE (payment_reference),
    CONSTRAINT chk_payment_transactions_non_negative_amount CHECK (amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_order_id
    ON payment_transactions (order_id);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_customer_ref
    ON payment_transactions (customer_ref);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_status
    ON payment_transactions (status);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_provider
    ON payment_transactions (provider_code, provider_transaction_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_transactions_order_idempotency
    ON payment_transactions (order_id, customer_ref, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

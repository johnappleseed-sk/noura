CREATE TABLE IF NOT EXISTS payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    payment_reference VARCHAR(64) NOT NULL,
    method_type VARCHAR(40) NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider_code VARCHAR(64) NOT NULL,
    provider_transaction_id VARCHAR(128),
    amount NUMERIC(14, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    failure_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_payment_transactions_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_transactions_payment_reference_ci
    ON payment_transactions (LOWER(payment_reference));

CREATE INDEX IF NOT EXISTS idx_payment_transactions_order_requested_at
    ON payment_transactions (order_id, requested_at DESC);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_status_requested_at
    ON payment_transactions (status, requested_at DESC);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_provider_transaction
    ON payment_transactions (LOWER(provider_code), provider_transaction_id);

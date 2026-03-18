CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS checkout_request_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_ref VARCHAR(180) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    status VARCHAR(24) NOT NULL,
    order_id UUID NULL,
    request_payload_json TEXT NULL,
    response_payload_json TEXT NULL,
    failure_code VARCHAR(80) NULL,
    failure_message VARCHAR(600) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(180) NULL,
    updated_by VARCHAR(180) NULL,
    CONSTRAINT uk_checkout_request_records_customer_key UNIQUE (customer_ref, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_checkout_request_records_status
    ON checkout_request_records (status);

CREATE INDEX IF NOT EXISTS idx_checkout_request_records_order_id
    ON checkout_request_records (order_id);

CREATE INDEX IF NOT EXISTS idx_checkout_request_records_updated_at
    ON checkout_request_records (updated_at DESC);

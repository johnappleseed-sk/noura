ALTER TABLE payment_transactions
    ADD COLUMN IF NOT EXISTS authorization_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUESTED',
    ADD COLUMN IF NOT EXISTS capture_status VARCHAR(32) NOT NULL DEFAULT 'NOT_CAPTURED',
    ADD COLUMN IF NOT EXISTS authorized_amount NUMERIC(14, 4) NULL,
    ADD COLUMN IF NOT EXISTS captured_amount NUMERIC(14, 4) NULL,
    ADD COLUMN IF NOT EXISTS refunded_amount NUMERIC(14, 4) NULL,
    ADD COLUMN IF NOT EXISTS auto_capture BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS last_webhook_received_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS last_webhook_processed_at TIMESTAMPTZ NULL;

UPDATE payment_transactions
SET status = CASE status
    WHEN 'INTENT_CREATED' THEN 'REQUIRES_CONFIRMATION'
    ELSE status
END
WHERE status = 'INTENT_CREATED';

UPDATE payment_transactions
SET authorization_status = CASE status
        WHEN 'AUTHORIZED' THEN 'AUTHORIZED'
        WHEN 'CAPTURED' THEN 'AUTHORIZED'
        WHEN 'REFUNDED' THEN 'AUTHORIZED'
        WHEN 'FAILED' THEN 'FAILED'
        WHEN 'CANCELED' THEN 'CANCELED'
        WHEN 'PENDING' THEN 'PENDING'
        ELSE 'NOT_REQUESTED'
    END,
    capture_status = CASE status
        WHEN 'CAPTURED' THEN 'CAPTURED'
        WHEN 'REFUNDED' THEN 'REFUNDED'
        WHEN 'FAILED' THEN CASE
            WHEN authorized_at IS NOT NULL THEN 'FAILED'
            ELSE 'NOT_CAPTURED'
        END
        WHEN 'CANCELED' THEN 'CANCELED'
        WHEN 'PENDING' THEN CASE
            WHEN authorized_at IS NOT NULL THEN 'PENDING'
            ELSE 'NOT_CAPTURED'
        END
        ELSE 'NOT_CAPTURED'
    END,
    authorized_amount = CASE
        WHEN status IN ('AUTHORIZED', 'CAPTURED', 'REFUNDED')
            THEN COALESCE(authorized_amount, amount)
        ELSE authorized_amount
    END,
    captured_amount = CASE
        WHEN status IN ('CAPTURED', 'REFUNDED')
            THEN COALESCE(captured_amount, amount)
        ELSE captured_amount
    END,
    refunded_amount = CASE
        WHEN status = 'REFUNDED'
            THEN COALESCE(refunded_amount, captured_amount, amount)
        ELSE refunded_amount
    END
WHERE TRUE;

CREATE TABLE IF NOT EXISTS payment_webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_transaction_id UUID NULL,
    provider_code VARCHAR(64) NOT NULL,
    provider_event_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payment_reference VARCHAR(64) NULL,
    provider_transaction_id VARCHAR(128) NULL,
    processing_status VARCHAR(32) NOT NULL,
    signature_verified BOOLEAN NOT NULL DEFAULT FALSE,
    payload_json TEXT NOT NULL,
    failure_reason VARCHAR(500) NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(180) NULL,
    updated_by VARCHAR(180) NULL,
    CONSTRAINT fk_payment_webhook_events_payment
        FOREIGN KEY (payment_transaction_id) REFERENCES payment_transactions (id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_webhook_events_provider_event
    ON payment_webhook_events (provider_code, provider_event_id);

CREATE INDEX IF NOT EXISTS idx_payment_webhook_events_payment
    ON payment_webhook_events (payment_transaction_id);

CREATE INDEX IF NOT EXISTS idx_payment_webhook_events_reference
    ON payment_webhook_events (payment_reference);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_webhook_timestamps
    ON payment_transactions (last_webhook_received_at DESC, last_webhook_processed_at DESC);

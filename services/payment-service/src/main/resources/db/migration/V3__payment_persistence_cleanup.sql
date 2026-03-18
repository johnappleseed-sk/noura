CREATE INDEX IF NOT EXISTS idx_payment_transactions_order_updated_at
    ON payment_transactions (order_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_order_customer_updated_at
    ON payment_transactions (order_id, customer_ref, updated_at DESC);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_payment_transactions_component_amounts_non_negative'
    ) THEN
        ALTER TABLE payment_transactions
            ADD CONSTRAINT chk_payment_transactions_component_amounts_non_negative
                CHECK (
                    (authorized_amount IS NULL OR authorized_amount >= 0)
                    AND (captured_amount IS NULL OR captured_amount >= 0)
                    AND (refunded_amount IS NULL OR refunded_amount >= 0)
                );
    END IF;
END $$;

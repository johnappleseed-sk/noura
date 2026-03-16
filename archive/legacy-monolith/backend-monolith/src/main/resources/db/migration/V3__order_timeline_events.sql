CREATE TABLE IF NOT EXISTS order_timeline_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    status VARCHAR(40) NOT NULL,
    refund_status VARCHAR(40) NOT NULL,
    actor VARCHAR(255),
    note VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_order_timeline_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_timeline_order_created_at
    ON order_timeline_events (order_id, created_at ASC);


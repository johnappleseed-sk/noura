CREATE TABLE product_reviews (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(180),
    updated_by VARCHAR(180),
    product_id UUID NOT NULL,
    customer_ref VARCHAR(180) NOT NULL,
    customer_name VARCHAR(180),
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title VARCHAR(180),
    comment VARCHAR(2000) NOT NULL,
    moderation_status VARCHAR(24) NOT NULL,
    moderation_notes VARCHAR(1000),
    submission_ip_hash VARCHAR(64),
    submission_user_agent_hash VARCHAR(64),
    spam_signals_json TEXT NOT NULL DEFAULT '{}',
    moderated_at TIMESTAMPTZ,
    moderated_by VARCHAR(180),
    approved_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX ux_product_reviews_product_customer
    ON product_reviews (product_id, customer_ref);

CREATE INDEX ix_product_reviews_product_status_created
    ON product_reviews (product_id, moderation_status, created_at DESC);

CREATE INDEX ix_product_reviews_status_created
    ON product_reviews (moderation_status, created_at DESC);

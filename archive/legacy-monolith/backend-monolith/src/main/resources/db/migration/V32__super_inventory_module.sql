CREATE TABLE IF NOT EXISTS product_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL,
    store_id UUID NOT NULL,
    proposed_name VARCHAR(255) NOT NULL,
    proposed_brand VARCHAR(255),
    proposed_category_code VARCHAR(80),
    proposed_attributes_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    proposed_barcode VARCHAR(64),
    proposed_sku VARCHAR(120),
    similarity_hash VARCHAR(64) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING_REVIEW',
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMPTZ,
    reviewed_by VARCHAR(255),
    review_notes VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_product_submissions_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE RESTRICT,
    CONSTRAINT fk_product_submissions_store FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_product_submissions_merchant_id
    ON product_submissions (merchant_id);

CREATE INDEX IF NOT EXISTS idx_product_submissions_store_id
    ON product_submissions (store_id);

CREATE INDEX IF NOT EXISTS idx_product_submissions_status
    ON product_submissions (status);

CREATE INDEX IF NOT EXISTS idx_product_submissions_proposed_barcode
    ON product_submissions (proposed_barcode);

CREATE INDEX IF NOT EXISTS idx_product_submissions_similarity_hash
    ON product_submissions (similarity_hash);

CREATE INDEX IF NOT EXISTS idx_product_submissions_submitted_at
    ON product_submissions (submitted_at DESC);

CREATE TABLE IF NOT EXISTS product_approval_decisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL,
    decision_type VARCHAR(40) NOT NULL,
    target_product_id UUID,
    notes VARCHAR(1000),
    decided_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    decided_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_product_approval_decisions_submission FOREIGN KEY (submission_id) REFERENCES product_submissions (id) ON DELETE CASCADE,
    CONSTRAINT fk_product_approval_decisions_target_product FOREIGN KEY (target_product_id) REFERENCES products (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_product_approval_decisions_submission
    ON product_approval_decisions (submission_id, decided_at DESC);

CREATE TABLE IF NOT EXISTS store_product_references (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL,
    product_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_store_product_references_store FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE RESTRICT,
    CONSTRAINT fk_store_product_references_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT,
    CONSTRAINT uk_store_product_references_store_product UNIQUE (store_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_store_product_references_store_active
    ON store_product_references (store_id, active);

CREATE INDEX IF NOT EXISTS idx_store_product_references_product_active
    ON store_product_references (product_id, active);

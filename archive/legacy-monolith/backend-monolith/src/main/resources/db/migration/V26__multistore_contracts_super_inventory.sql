-- Multi-store contract onboarding + Super Inventory governance foundations.
-- Notes:
-- - Existing `products` table remains the canonical master catalog.
-- - `product_inventory` acts as the store-adoption layer with store-specific overrides.

-- ── Merchants / Partners ───────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS merchants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    tax_id VARCHAR(80),
    primary_email VARCHAR(255),
    primary_phone VARCHAR(40),
    status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    notes VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_merchants_name
    ON merchants (LOWER(name));

CREATE INDEX IF NOT EXISTS idx_merchants_status_created
    ON merchants (status, created_at DESC);

-- ── Contracts ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS merchant_contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL,
    contract_number VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
    start_date DATE NOT NULL,
    end_date DATE,
    requested_by_user_id UUID,
    reviewed_by_user_id UUID,
    reviewed_at TIMESTAMPTZ,
    review_note VARCHAR(1000),
    terms_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_merchant_contracts_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE CASCADE,
    CONSTRAINT fk_merchant_contracts_requested_by FOREIGN KEY (requested_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_merchant_contracts_reviewed_by FOREIGN KEY (reviewed_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT uk_merchant_contracts_contract_number UNIQUE (contract_number)
);

CREATE INDEX IF NOT EXISTS idx_merchant_contracts_merchant_created
    ON merchant_contracts (merchant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_merchant_contracts_status_dates
    ON merchant_contracts (status, start_date, end_date);

CREATE TABLE IF NOT EXISTS merchant_contract_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL,
    action VARCHAR(40) NOT NULL,
    actor_email VARCHAR(255),
    actor_user_id UUID,
    note VARCHAR(1000),
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    correlation_id VARCHAR(120),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_merchant_contract_actions_contract FOREIGN KEY (contract_id) REFERENCES merchant_contracts (id) ON DELETE CASCADE,
    CONSTRAINT fk_merchant_contract_actions_actor FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_merchant_contract_actions_contract_occurred
    ON merchant_contract_actions (contract_id, occurred_at DESC);

-- ── Store Tenant Registration ──────────────────────────────────────────

CREATE TABLE IF NOT EXISTS store_tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL UNIQUE,
    merchant_id UUID NOT NULL,
    contract_id UUID NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    activated_at TIMESTAMPTZ,
    deactivated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_store_tenants_store FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE,
    CONSTRAINT fk_store_tenants_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE CASCADE,
    CONSTRAINT fk_store_tenants_contract FOREIGN KEY (contract_id) REFERENCES merchant_contracts (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_store_tenants_merchant
    ON store_tenants (merchant_id);

CREATE INDEX IF NOT EXISTS idx_store_tenants_contract
    ON store_tenants (contract_id);

CREATE INDEX IF NOT EXISTS idx_store_tenants_status
    ON store_tenants (status);

-- ── Store Staff Assignments (Tenant access) ────────────────────────────

CREATE TABLE IF NOT EXISTS user_store_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    store_id UUID NOT NULL,
    role_code VARCHAR(80),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_user_store_assignments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_store_assignments_store FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE,
    CONSTRAINT uk_user_store_assignments_user_store UNIQUE (user_id, store_id)
);

CREATE INDEX IF NOT EXISTS idx_user_store_assignments_store_active
    ON user_store_assignments (store_id, active);

CREATE INDEX IF NOT EXISTS idx_user_store_assignments_user_active
    ON user_store_assignments (user_id, active);

-- ── Warehouses: optional store ownership for per-branch inventory ──────

ALTER TABLE warehouses
    ADD COLUMN IF NOT EXISTS store_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_warehouses_store') THEN
        ALTER TABLE warehouses
            ADD CONSTRAINT fk_warehouses_store
            FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_warehouses_store
    ON warehouses (store_id);

-- ── Store adoption / overrides (extends existing product_inventory) ────

ALTER TABLE product_inventory
    ADD COLUMN IF NOT EXISTS published BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS visible BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS local_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS local_description VARCHAR(5000),
    ADD COLUMN IF NOT EXISTS tax_code VARCHAR(80);

CREATE INDEX IF NOT EXISTS idx_product_inventory_store_published
    ON product_inventory (store_id, published);

-- ── Master products: dedupe helpers (keeps products as Super Inventory) ─

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS manufacturer_part_number VARCHAR(80),
    ADD COLUMN IF NOT EXISTS normalized_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS dedupe_fingerprint VARCHAR(64);

UPDATE products
SET normalized_name = LOWER(REGEXP_REPLACE(TRIM(name), '\\s+', ' ', 'g'))
WHERE normalized_name IS NULL;

CREATE INDEX IF NOT EXISTS idx_products_normalized_name
    ON products (normalized_name);

CREATE INDEX IF NOT EXISTS idx_products_mpn
    ON products (manufacturer_part_number);

CREATE INDEX IF NOT EXISTS idx_products_dedupe_fingerprint
    ON products (dedupe_fingerprint);

-- ── Product submissions + review workflow ──────────────────────────────

CREATE TABLE IF NOT EXISTS product_submission_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL,
    merchant_id UUID,
    parent_submission_id UUID,
    revision_number INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING_REVIEW',
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    normalized_name VARCHAR(255),
    barcode VARCHAR(32),
    manufacturer_part_number VARCHAR(80),
    dedupe_fingerprint VARCHAR(64),
    potential_duplicate BOOLEAN NOT NULL DEFAULT FALSE,
    matched_master_product_id UUID,
    requested_by_user_id UUID NOT NULL,
    reviewed_by_user_id UUID,
    reviewed_at TIMESTAMPTZ,
    review_note VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_product_submission_store FOREIGN KEY (store_id) REFERENCES stores (id) ON DELETE CASCADE,
    CONSTRAINT fk_product_submission_merchant FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE SET NULL,
    CONSTRAINT fk_product_submission_parent FOREIGN KEY (parent_submission_id) REFERENCES product_submission_requests (id) ON DELETE SET NULL,
    CONSTRAINT fk_product_submission_requested_by FOREIGN KEY (requested_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_product_submission_reviewed_by FOREIGN KEY (reviewed_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_product_submission_matched_master FOREIGN KEY (matched_master_product_id) REFERENCES products (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_product_submission_status_created
    ON product_submission_requests (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_product_submission_store_status_created
    ON product_submission_requests (store_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_product_submission_fingerprint
    ON product_submission_requests (dedupe_fingerprint);

CREATE TABLE IF NOT EXISTS product_submission_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL,
    action VARCHAR(40) NOT NULL,
    reviewer_user_id UUID,
    reviewer_email VARCHAR(255),
    note VARCHAR(1000),
    master_product_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_product_submission_reviews_submission FOREIGN KEY (submission_id) REFERENCES product_submission_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_product_submission_reviews_reviewer FOREIGN KEY (reviewer_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_product_submission_reviews_master FOREIGN KEY (master_product_id) REFERENCES products (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_product_submission_reviews_submission_occurred
    ON product_submission_reviews (submission_id, occurred_at DESC);

CREATE TABLE IF NOT EXISTS product_dedupe_candidates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL,
    master_product_id UUID NOT NULL,
    match_score NUMERIC(5, 4) NOT NULL DEFAULT 0,
    match_reason VARCHAR(120) NOT NULL,
    detail_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_product_dedupe_candidates_submission FOREIGN KEY (submission_id) REFERENCES product_submission_requests (id) ON DELETE CASCADE,
    CONSTRAINT fk_product_dedupe_candidates_master FOREIGN KEY (master_product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT uk_product_dedupe_candidates_submission_master UNIQUE (submission_id, master_product_id)
);

CREATE INDEX IF NOT EXISTS idx_product_dedupe_candidates_submission_score
    ON product_dedupe_candidates (submission_id, match_score DESC);

-- ── RBAC: add first-class contract + submission permissions ────────────

INSERT INTO admin_permissions (scope, action, label, description, module_group, display_order, is_sensitive, created_at, created_by)
VALUES
    ('contracts', 'read', 'Contracts Read', 'View merchant contracts and store registrations.', 'governance', 160, FALSE, NOW(), 'flyway'),
    ('contracts', 'create', 'Contracts Create', 'Create merchants and draft contracts.', 'governance', 161, TRUE, NOW(), 'flyway'),
    ('contracts', 'update', 'Contracts Update', 'Update and suspend active contracts.', 'governance', 162, TRUE, NOW(), 'flyway'),
    ('contracts', 'approve', 'Contracts Approve', 'Approve, reject, or terminate contracts.', 'governance', 163, TRUE, NOW(), 'flyway'),
    ('product_submissions', 'read', 'Product Submissions Read', 'View product submission requests and dedupe signals.', 'governance', 170, FALSE, NOW(), 'flyway'),
    ('product_submissions', 'approve', 'Product Submissions Approve', 'Approve/reject product submissions and link duplicates.', 'governance', 171, TRUE, NOW(), 'flyway')
ON CONFLICT (scope, action) DO UPDATE
SET
    label = EXCLUDED.label,
    description = EXCLUDED.description,
    module_group = EXCLUDED.module_group,
    display_order = EXCLUDED.display_order,
    is_sensitive = EXCLUDED.is_sensitive;

INSERT INTO admin_role_permissions (role_id, permission_id, created_at, created_by)
SELECT role.id, permission.id, NOW(), 'flyway'
FROM admin_roles role
JOIN admin_permissions permission
    ON (permission.scope = 'contracts' AND permission.action IN ('read','create','update','approve'))
    OR (permission.scope = 'product_submissions' AND permission.action IN ('read','approve'))
WHERE role.code IN ('SUPER_ADMIN', 'ADMIN')
ON CONFLICT (role_id, permission_id) DO NOTHING;


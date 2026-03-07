ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS manager_id UUID,
    ADD COLUMN IF NOT EXISTS classification_code VARCHAR(120),
    ADD COLUMN IF NOT EXISTS taxonomy_version INTEGER NOT NULL DEFAULT 1;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_categories_manager'
    ) THEN
        ALTER TABLE categories
            ADD CONSTRAINT fk_categories_manager
                FOREIGN KEY (manager_id) REFERENCES users (id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS category_translations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL,
    locale VARCHAR(16) NOT NULL,
    localized_name VARCHAR(255) NOT NULL,
    localized_description VARCHAR(255),
    seo_slug VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_category_translations_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE,
    CONSTRAINT uk_category_translation_category_locale UNIQUE (category_id, locale)
);

CREATE TABLE IF NOT EXISTS channel_category_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL,
    channel VARCHAR(60) NOT NULL,
    region_code VARCHAR(16) NOT NULL DEFAULT 'GLOBAL',
    external_category_id VARCHAR(255) NOT NULL,
    external_category_name VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_channel_category_mappings_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE,
    CONSTRAINT uk_channel_category_mapping_category_channel_region UNIQUE (category_id, channel, region_code)
);

CREATE TABLE IF NOT EXISTS category_change_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID,
    action VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    requested_by_user_id UUID NOT NULL,
    reviewed_by_user_id UUID,
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    reason VARCHAR(1000),
    review_comment VARCHAR(1000),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_category_change_requests_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL,
    CONSTRAINT fk_category_change_requests_requested_by FOREIGN KEY (requested_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_category_change_requests_reviewed_by FOREIGN KEY (reviewed_by_user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_categories_manager ON categories (manager_id);
CREATE INDEX IF NOT EXISTS idx_categories_classification_code ON categories (classification_code);
CREATE INDEX IF NOT EXISTS idx_category_translations_locale ON category_translations (locale);
CREATE INDEX IF NOT EXISTS idx_channel_category_mappings_channel_region ON channel_category_mappings (channel, region_code);
CREATE INDEX IF NOT EXISTS idx_category_change_requests_status_created ON category_change_requests (status, created_at DESC);

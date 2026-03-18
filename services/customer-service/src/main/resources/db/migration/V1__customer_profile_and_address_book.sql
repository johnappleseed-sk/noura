CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS customer_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_subject VARCHAR(180) NOT NULL,
    email VARCHAR(255) NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(60) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    default_shipping_address_id UUID NULL,
    default_billing_address_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120) NULL,
    updated_by VARCHAR(120) NULL,
    CONSTRAINT uk_customer_profiles_external_subject UNIQUE (external_subject)
);

CREATE INDEX IF NOT EXISTS idx_customer_profiles_email
    ON customer_profiles (email);

CREATE TABLE IF NOT EXISTS customer_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    label VARCHAR(80) NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(60) NULL,
    line1 VARCHAR(255) NOT NULL,
    line2 VARCHAR(255) NULL,
    district VARCHAR(120) NULL,
    city VARCHAR(120) NOT NULL,
    state_province VARCHAR(120) NOT NULL,
    postal_code VARCHAR(30) NOT NULL,
    country_code VARCHAR(8) NOT NULL,
    latitude NUMERIC(10, 7) NULL,
    longitude NUMERIC(10, 7) NULL,
    accuracy_meters INTEGER NULL,
    place_id VARCHAR(220) NULL,
    formatted_address VARCHAR(1024) NULL,
    delivery_instructions VARCHAR(600) NULL,
    validation_status VARCHAR(32) NOT NULL DEFAULT 'UNVERIFIED',
    default_shipping BOOLEAN NOT NULL DEFAULT FALSE,
    default_billing BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120) NULL,
    updated_by VARCHAR(120) NULL,
    CONSTRAINT fk_customer_addresses_customer
        FOREIGN KEY (customer_id) REFERENCES customer_profiles (id) ON DELETE CASCADE,
    CONSTRAINT chk_customer_addresses_lat_lng_pair
        CHECK ((latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL))
);

CREATE INDEX IF NOT EXISTS idx_customer_addresses_customer_id
    ON customer_addresses (customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_addresses_customer_updated_at
    ON customer_addresses (customer_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_customer_addresses_validation_status
    ON customer_addresses (validation_status);

CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_addresses_default_shipping
    ON customer_addresses (customer_id)
    WHERE default_shipping = TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_addresses_default_billing
    ON customer_addresses (customer_id)
    WHERE default_billing = TRUE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_customer_profiles_default_shipping'
    ) THEN
        ALTER TABLE customer_profiles
            ADD CONSTRAINT fk_customer_profiles_default_shipping
                FOREIGN KEY (default_shipping_address_id) REFERENCES customer_addresses (id) ON DELETE SET NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_customer_profiles_default_billing'
    ) THEN
        ALTER TABLE customer_profiles
            ADD CONSTRAINT fk_customer_profiles_default_billing
                FOREIGN KEY (default_billing_address_id) REFERENCES customer_addresses (id) ON DELETE SET NULL;
    END IF;
END $$;

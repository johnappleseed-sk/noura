CREATE SCHEMA IF NOT EXISTS admin_governance;
CREATE SCHEMA IF NOT EXISTS merchant_network;
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS inventory;
CREATE SCHEMA IF NOT EXISTS search_meta;
CREATE SCHEMA IF NOT EXISTS commerce_core;
CREATE SCHEMA IF NOT EXISTS pricing;

CREATE TABLE IF NOT EXISTS admin_governance.platform_bootstrap_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id VARCHAR(255) NOT NULL,
    actor_type VARCHAR(64) NOT NULL,
    action VARCHAR(128) NOT NULL,
    entity_type VARCHAR(128) NOT NULL,
    entity_id VARCHAR(255),
    correlation_id VARCHAR(128),
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_platform_bootstrap_audit_log_created_at
    ON admin_governance.platform_bootstrap_audit_log (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_platform_bootstrap_audit_log_entity
    ON admin_governance.platform_bootstrap_audit_log (entity_type, entity_id);

CREATE TABLE IF NOT EXISTS catalog.outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(128) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_catalog_outbox_events_published_created_at
    ON catalog.outbox_events (published, created_at);

CREATE TABLE IF NOT EXISTS inventory.outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(128) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_inventory_outbox_events_published_created_at
    ON inventory.outbox_events (published, created_at);

CREATE TABLE IF NOT EXISTS search_meta.projection_checkpoints (
    projection_name VARCHAR(128) PRIMARY KEY,
    last_event_id UUID,
    last_processed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

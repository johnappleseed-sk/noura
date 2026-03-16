CREATE TABLE IF NOT EXISTS recovery_records (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_key VARCHAR(120) NOT NULL,
    entity_type VARCHAR(120) NOT NULL,
    entity_id VARCHAR(120) NOT NULL,
    display_name VARCHAR(255) NULL,
    lifecycle_state VARCHAR(40) NOT NULL,
    current_version_number INTEGER NOT NULL DEFAULT 0,
    last_version_id UUID NULL,
    backup_verified BOOLEAN NOT NULL DEFAULT FALSE,
    anonymized BOOLEAN NOT NULL DEFAULT FALSE,
    retention_until TIMESTAMPTZ NULL,
    legal_hold_until TIMESTAMPTZ NULL,
    deleted_at TIMESTAMPTZ NULL,
    archived_at TIMESTAMPTZ NULL,
    inactivated_at TIMESTAMPTZ NULL,
    restored_at TIMESTAMPTZ NULL,
    purged_at TIMESTAMPTZ NULL,
    last_action_by VARCHAR(255) NULL,
    last_reason VARCHAR(1000) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_recovery_record_scope UNIQUE (tenant_key, entity_type, entity_id)
);

CREATE INDEX IF NOT EXISTS idx_recovery_record_state ON recovery_records (tenant_key, lifecycle_state, updated_at);
CREATE INDEX IF NOT EXISTS idx_recovery_record_entity ON recovery_records (tenant_key, entity_type, entity_id);

CREATE TABLE IF NOT EXISTS recovery_versions (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    recovery_record_id UUID NULL,
    tenant_key VARCHAR(120) NOT NULL,
    entity_type VARCHAR(120) NOT NULL,
    entity_id VARCHAR(120) NOT NULL,
    version_number INTEGER NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    lifecycle_state_after VARCHAR(40) NOT NULL,
    captured_at TIMESTAMPTZ NOT NULL,
    actor VARCHAR(255) NULL,
    reason VARCHAR(1000) NULL,
    backup_snapshot BOOLEAN NOT NULL DEFAULT FALSE,
    anonymized BOOLEAN NOT NULL DEFAULT FALSE,
    restored_from_at TIMESTAMPTZ NULL,
    snapshot_json TEXT NOT NULL,
    metadata_json TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_recovery_versions_record FOREIGN KEY (recovery_record_id) REFERENCES recovery_records (id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_recovery_version_scope ON recovery_versions (tenant_key, entity_type, entity_id, version_number);
CREATE INDEX IF NOT EXISTS idx_recovery_version_time ON recovery_versions (tenant_key, entity_type, entity_id, captured_at DESC);

CREATE TABLE IF NOT EXISTS recovery_audit_logs (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_key VARCHAR(120) NOT NULL,
    entity_type VARCHAR(120) NOT NULL,
    entity_id VARCHAR(120) NULL,
    action_type VARCHAR(40) NOT NULL,
    action_status VARCHAR(40) NOT NULL,
    actor VARCHAR(255) NULL,
    correlation_id VARCHAR(120) NULL,
    message VARCHAR(1000) NULL,
    metadata_json TEXT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by VARCHAR(255) NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_recovery_audit_scope ON recovery_audit_logs (tenant_key, entity_type, action_type, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_recovery_audit_status ON recovery_audit_logs (tenant_key, action_status, occurred_at DESC);

CREATE TABLE IF NOT EXISTS recovery_action_jobs (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_key VARCHAR(120) NOT NULL,
    entity_type VARCHAR(120) NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    requested_by VARCHAR(255) NULL,
    dry_run BOOLEAN NOT NULL DEFAULT FALSE,
    total_items INTEGER NOT NULL DEFAULT 0,
    processed_items INTEGER NOT NULL DEFAULT 0,
    success_items INTEGER NOT NULL DEFAULT 0,
    failed_items INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ NULL,
    completed_at TIMESTAMPTZ NULL,
    validation_summary_json TEXT NULL,
    result_summary_json TEXT NULL,
    error_summary VARCHAR(1000) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by VARCHAR(255) NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_recovery_job_scope ON recovery_action_jobs (tenant_key, entity_type, status, updated_at DESC);

-- Recovery center: approvals + job payload storage.
-- Adds persisted request payload for job retries and a 4-eyes approval workflow for high-impact actions.

ALTER TABLE recovery_action_jobs
    ADD COLUMN IF NOT EXISTS request_payload_json TEXT NULL;

CREATE TABLE IF NOT EXISTS recovery_action_approvals (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_key VARCHAR(120) NOT NULL,
    request_kind VARCHAR(40) NOT NULL,
    entity_type VARCHAR(120) NOT NULL,
    entity_id VARCHAR(120) NULL,
    action_type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    requested_items INTEGER NOT NULL DEFAULT 1,
    reason VARCHAR(1000) NULL,
    change_ticket VARCHAR(255) NULL,
    requested_by VARCHAR(255) NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_by VARCHAR(255) NULL,
    reviewed_at TIMESTAMPTZ NULL,
    reviewer_notes VARCHAR(1000) NULL,
    executed_job_id UUID NULL,
    executed_at TIMESTAMPTZ NULL,
    execution_error VARCHAR(1000) NULL,
    request_payload_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NULL,
    created_by VARCHAR(255) NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_recovery_approval_scope
    ON recovery_action_approvals (tenant_key, status, requested_at DESC);

CREATE INDEX IF NOT EXISTS idx_recovery_approval_entity
    ON recovery_action_approvals (tenant_key, entity_type, entity_id);


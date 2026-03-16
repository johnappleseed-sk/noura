CREATE TABLE IF NOT EXISTS admin_rbac_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action_type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(120),
    actor_email VARCHAR(255),
    actor_user_id UUID,
    outcome VARCHAR(40) NOT NULL,
    correlation_id VARCHAR(120),
    details_json TEXT,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_admin_rbac_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_admin_rbac_audit_logs_occurred_at
    ON admin_rbac_audit_logs (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_rbac_audit_logs_action
    ON admin_rbac_audit_logs (action_type);
CREATE INDEX IF NOT EXISTS idx_admin_rbac_audit_logs_entity
    ON admin_rbac_audit_logs (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_admin_rbac_audit_logs_actor
    ON admin_rbac_audit_logs (actor_user_id);


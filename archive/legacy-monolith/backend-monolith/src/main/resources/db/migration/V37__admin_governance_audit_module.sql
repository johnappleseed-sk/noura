CREATE TABLE IF NOT EXISTS audit_log_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID,
    actor_username VARCHAR(255),
    action_code VARCHAR(120) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    old_value_json TEXT,
    new_value_json TEXT,
    request_path VARCHAR(512),
    request_method VARCHAR(16),
    ip_address VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_audit_log_entries_actor
        FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_log_entries_created_at
    ON audit_log_entries (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_log_entries_actor_username
    ON audit_log_entries (actor_username);

CREATE INDEX IF NOT EXISTS idx_audit_log_entries_action_code
    ON audit_log_entries (action_code);

CREATE INDEX IF NOT EXISTS idx_audit_log_entries_entity
    ON audit_log_entries (entity_type, entity_id);

CREATE OR REPLACE FUNCTION prevent_audit_log_entry_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'audit_log_entries is immutable';
END;
$$;

DROP TRIGGER IF EXISTS trg_audit_log_entries_no_update ON audit_log_entries;
CREATE TRIGGER trg_audit_log_entries_no_update
BEFORE UPDATE ON audit_log_entries
FOR EACH ROW
EXECUTE FUNCTION prevent_audit_log_entry_mutation();

DROP TRIGGER IF EXISTS trg_audit_log_entries_no_delete ON audit_log_entries;
CREATE TRIGGER trg_audit_log_entries_no_delete
BEFORE DELETE ON audit_log_entries
FOR EACH ROW
EXECUTE FUNCTION prevent_audit_log_entry_mutation();

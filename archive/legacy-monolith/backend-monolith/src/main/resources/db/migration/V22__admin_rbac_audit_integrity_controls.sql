ALTER TABLE admin_rbac_audit_logs
    ADD COLUMN IF NOT EXISTS payload_hash VARCHAR(64);

UPDATE admin_rbac_audit_logs
SET payload_hash = encode(
        digest(
                COALESCE(action_type, '') || '|' ||
                COALESCE(entity_type, '') || '|' ||
                COALESCE(entity_id, '') || '|' ||
                COALESCE(actor_email, '') || '|' ||
                COALESCE(outcome, '') || '|' ||
                COALESCE(correlation_id, '') || '|' ||
                COALESCE(details_json, '') || '|' ||
                EXTRACT(EPOCH FROM occurred_at)::BIGINT::TEXT,
                'sha256'
        ),
        'hex'
    )
WHERE payload_hash IS NULL OR payload_hash = '';

ALTER TABLE admin_rbac_audit_logs
    ALTER COLUMN payload_hash SET NOT NULL;

CREATE OR REPLACE FUNCTION prevent_admin_rbac_audit_log_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'admin_rbac_audit_logs is immutable';
END;
$$;

DROP TRIGGER IF EXISTS trg_admin_rbac_audit_logs_no_update ON admin_rbac_audit_logs;
CREATE TRIGGER trg_admin_rbac_audit_logs_no_update
BEFORE UPDATE ON admin_rbac_audit_logs
FOR EACH ROW
EXECUTE FUNCTION prevent_admin_rbac_audit_log_mutation();

DROP TRIGGER IF EXISTS trg_admin_rbac_audit_logs_no_delete ON admin_rbac_audit_logs;
CREATE TRIGGER trg_admin_rbac_audit_logs_no_delete
BEFORE DELETE ON admin_rbac_audit_logs
FOR EACH ROW
EXECUTE FUNCTION prevent_admin_rbac_audit_log_mutation();


CREATE TABLE IF NOT EXISTS admin_bulk_user_role_views (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    query_text VARCHAR(255),
    user_ids_json TEXT NOT NULL DEFAULT '[]',
    role_codes_json TEXT NOT NULL DEFAULT '[]',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT fk_admin_bulk_user_role_views_owner
        FOREIGN KEY (owner_user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_admin_bulk_user_role_views_owner_name
    ON admin_bulk_user_role_views (owner_user_id, lower(name));

CREATE INDEX IF NOT EXISTS idx_admin_bulk_user_role_views_owner
    ON admin_bulk_user_role_views (owner_user_id);

-- Store management admin fields, governance keys, and search indexes.

ALTER TABLE stores
    ADD COLUMN IF NOT EXISTS store_code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS merchant_id UUID,
    ADD COLUMN IF NOT EXISTS slug VARCHAR(255),
    ADD COLUMN IF NOT EXISTS type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS contact_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(40),
    ADD COLUMN IF NOT EXISTS country_code VARCHAR(12),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

DO $$
DECLARE
    v_default_merchant UUID;
BEGIN
    UPDATE stores
    SET store_code = COALESCE(
        NULLIF(BTRIM(UPPER(store_code)), ''),
        'STORE-' || UPPER(SUBSTRING(REPLACE(id::text, '-', '') FROM 1 FOR 12))
    );

    UPDATE stores
    SET slug = COALESCE(
        NULLIF(BTRIM(slug), ''),
        NULLIF(LOWER(
            REGEXP_REPLACE(
                BTRIM(LOWER(name)),
                '[^a-z0-9]+', '-', 'g'
            )
        ), '')
    );

    UPDATE stores
    SET slug = NULLIF(
        REGEXP_REPLACE(
            REGEXP_REPLACE(
                REGEXP_REPLACE(
                    COALESCE(slug, ''),
                    '^-+|-+$', '', 'g'
                ),
                '-{2,}', '-', 'g'
            ),
            '-$', '', 'g'
        ),
        ''
    );

    UPDATE stores
    SET slug = 'store-' || SUBSTRING(REPLACE(id::text, '-', '') FROM 1 FOR 24)
    WHERE slug IS NULL;

    WITH normalized AS (
        SELECT
            id,
            COALESCE(
                NULLIF(BTRIM(UPPER(store_code)), ''),
                'STORE-' || UPPER(SUBSTRING(REPLACE(id::text, '-', '') FROM 1 FOR 12))
            ) AS base_store_code
        FROM stores
    ),
    ranked AS (
        SELECT
            id,
            base_store_code,
            ROW_NUMBER() OVER (
                PARTITION BY base_store_code
                ORDER BY created_at, id
            ) AS seq
        FROM normalized
    )
    UPDATE stores s
    SET store_code = CASE
                         WHEN r.seq = 1 THEN r.base_store_code
                         ELSE LEFT(r.base_store_code, 72) || '-' || LPAD(r.seq::text, 3, '0')
                     END
    FROM ranked r
    WHERE s.id = r.id;

    WITH normalized AS (
        SELECT
            id,
            LEFT(
                COALESCE(
                    NULLIF(BTRIM(REGEXP_REPLACE(
                        REGEXP_REPLACE(
                            REGEXP_REPLACE(LOWER(BTRIM(COALESCE(name, 'store'))), '[^a-z0-9]+', '-', 'g'),
                            '-{2,}',
                            '-',
                            'g'
                        ),
                        '^-+|-+$',
                        '',
                        'g'
                    )),
                    ''),
                    'store-' || SUBSTRING(REPLACE(id::text, '-', '') FROM 1 FOR 12)
                ),
                220
            ) AS base_slug
        FROM stores
    ),
    ranked AS (
        SELECT
            id,
            base_slug,
            ROW_NUMBER() OVER (
                PARTITION BY base_slug
                ORDER BY created_at, id
            ) AS seq
        FROM normalized
    )
    UPDATE stores s
    SET slug = CASE
                   WHEN r.seq = 1 THEN r.base_slug
                   ELSE LEFT(r.base_slug, 212) || '-' || LPAD(r.seq::text, 3, '0')
               END
    FROM ranked r
    WHERE s.id = r.id;

    UPDATE stores
    SET merchant_id = st.merchant_id
    FROM store_tenants st
    WHERE st.store_id = stores.id
      AND stores.merchant_id IS NULL;

    SELECT id INTO v_default_merchant
    FROM merchants
    ORDER BY created_at
    LIMIT 1;

    IF v_default_merchant IS NOT NULL THEN
        UPDATE stores
        SET merchant_id = v_default_merchant
        WHERE merchant_id IS NULL;
    END IF;
END $$;

UPDATE stores
SET type = COALESCE(NULLIF(BTRIM(type), ''), 'MERCHANT'),
    status = COALESCE(NULLIF(BTRIM(status), ''), 'ACTIVE'),
    country_code = NULLIF(BTRIM(UPPER(country_code)), '')
WHERE TRUE;

UPDATE stores
SET contact_email = NULLIF(BTRIM(LOWER(contact_email)), '')
WHERE contact_email IS NOT NULL;

UPDATE stores
SET contact_phone = NULLIF(BTRIM(contact_phone), '')
WHERE contact_phone IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_stores_merchant'
    ) THEN
        ALTER TABLE stores
            ADD CONSTRAINT fk_stores_merchant
            FOREIGN KEY (merchant_id) REFERENCES merchants (id) ON DELETE RESTRICT;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_stores_store_code_ci
    ON stores (LOWER(store_code));

CREATE UNIQUE INDEX IF NOT EXISTS uk_stores_slug_ci
    ON stores (LOWER(slug));

CREATE INDEX IF NOT EXISTS idx_stores_merchant
    ON stores (merchant_id);

CREATE INDEX IF NOT EXISTS idx_stores_status
    ON stores (status);

CREATE INDEX IF NOT EXISTS idx_stores_type
    ON stores (type);

CREATE INDEX IF NOT EXISTS idx_stores_merchant_type_status
    ON stores (merchant_id, type, status);

ALTER TABLE stores
    ALTER COLUMN store_code SET NOT NULL,
    ALTER COLUMN slug SET NOT NULL,
    ALTER COLUMN type SET NOT NULL,
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE stores
    ALTER COLUMN type SET DEFAULT 'MERCHANT',
    ALTER COLUMN status SET DEFAULT 'ACTIVE';

-- Store RBAC capabilities (for admin permission checks that use PERM_STORES_* authorities).
INSERT INTO admin_permissions (scope, action, label, description, created_at, created_by)
VALUES
    ('stores', 'read', 'Stores Read', 'Allows read access to stores.', NOW(), 'flyway'),
    ('stores', 'create', 'Stores Create', 'Allows creation of stores.', NOW(), 'flyway'),
    ('stores', 'update', 'Stores Update', 'Allows update operations for stores.', NOW(), 'flyway'),
    ('stores', 'delete', 'Stores Delete', 'Allows deletion/deactivation of stores.', NOW(), 'flyway'),
    ('stores', 'approve', 'Stores Approve', 'Allows store approval and policy decisions.', NOW(), 'flyway')
ON CONFLICT (scope, action) DO NOTHING;

INSERT INTO admin_role_permissions (role_id, permission_id, created_at, created_by)
SELECT role.id, perm.id, NOW(), 'flyway'
FROM admin_roles role
JOIN admin_permissions perm
    ON perm.scope = 'stores'
   AND perm.action IN ('read', 'create', 'update', 'approve')
WHERE role.code IN ('ADMIN', 'SUPER_ADMIN')
  AND role.active = TRUE
ON CONFLICT (role_id, permission_id) DO NOTHING;

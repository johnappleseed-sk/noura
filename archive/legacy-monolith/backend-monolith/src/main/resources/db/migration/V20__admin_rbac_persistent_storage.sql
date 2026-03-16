CREATE TABLE IF NOT EXISTS admin_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(64) NOT NULL UNIQUE,
    label VARCHAR(255) NOT NULL,
    description VARCHAR(600),
    is_system_role BOOLEAN NOT NULL DEFAULT FALSE,
    assignable BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS admin_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope VARCHAR(80) NOT NULL,
    action VARCHAR(40) NOT NULL,
    label VARCHAR(255) NOT NULL,
    description VARCHAR(600),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    created_by VARCHAR(255),
    CONSTRAINT uk_admin_permissions_scope_action UNIQUE (scope, action)
);

CREATE TABLE IF NOT EXISTS admin_role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_admin_role_permissions_role FOREIGN KEY (role_id) REFERENCES admin_roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_admin_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES admin_permissions (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS admin_user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_admin_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_admin_user_roles_role FOREIGN KEY (role_id) REFERENCES admin_roles (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_admin_roles_code ON admin_roles (code);
CREATE INDEX IF NOT EXISTS idx_admin_roles_active ON admin_roles (active);
CREATE INDEX IF NOT EXISTS idx_admin_permissions_scope ON admin_permissions (scope);
CREATE INDEX IF NOT EXISTS idx_admin_user_roles_role ON admin_user_roles (role_id);

WITH scope_catalog(scope) AS (
    VALUES
        ('products'),
        ('categories'),
        ('media'),
        ('orders'),
        ('customers'),
        ('users'),
        ('staff'),
        ('roles'),
        ('inventory'),
        ('discounts'),
        ('reviews'),
        ('reports'),
        ('settings'),
        ('integrations'),
        ('audit_logs')
),
action_catalog(action) AS (
    VALUES ('read'), ('create'), ('update'), ('delete'), ('approve'), ('export')
)
INSERT INTO admin_permissions (scope, action, label, description, created_at, created_by)
SELECT
    scope_catalog.scope,
    action_catalog.action,
    initcap(replace(scope_catalog.scope, '_', ' ')) || ' ' || initcap(action_catalog.action),
    'Allows ' || action_catalog.action || ' access in ' || scope_catalog.scope || ' scope.',
    NOW(),
    'flyway'
FROM scope_catalog
CROSS JOIN action_catalog
ON CONFLICT (scope, action) DO NOTHING;

INSERT INTO admin_roles (code, label, description, is_system_role, assignable, active, created_at, created_by)
VALUES
    ('SUPER_ADMIN', 'Super Admin', 'Unrestricted governance authority with full operational and compliance access.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('ADMIN', 'Admin', 'Primary operations administrator for commerce and warehouse modules.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('MANAGER', 'Manager', 'Cross-functional manager for commerce operations and reporting.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('CONTENT_MANAGER', 'Content Manager', 'Owns CMS, merchandising narratives, and content quality controls.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('PRODUCT_MANAGER', 'Product Manager', 'Owns product lifecycle and catalog quality governance.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('INVENTORY_MANAGER', 'Inventory Manager', 'Owns stock accuracy, inventory adjustments, and warehouse planning.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('ORDER_MANAGER', 'Order Manager', 'Owns order workflow and status transition quality.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('SUPPORT_AGENT', 'Support Agent', 'Handles customer support and returns triage.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('FINANCE', 'Finance', 'Owns discount governance, commercial policy, and exports.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('ANALYST', 'Analyst', 'Read-first access to reports and governance metrics.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('STAFF', 'Staff', 'General operations staff with limited read-oriented access.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('WAREHOUSE_MANAGER', 'Warehouse Manager', 'Inventory and movement manager in warehouse operations.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('VIEWER', 'Viewer', 'Read-only warehouse user for monitoring visibility.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('CUSTOMER', 'Customer', 'Customer-facing role for compatibility.', TRUE, FALSE, TRUE, NOW(), 'flyway'),
    ('B2B', 'B2B', 'Business customer role for compatibility.', TRUE, FALSE, TRUE, NOW(), 'flyway')
ON CONFLICT (code) DO UPDATE
SET
    label = EXCLUDED.label,
    description = EXCLUDED.description,
    is_system_role = EXCLUDED.is_system_role,
    assignable = EXCLUDED.assignable,
    active = EXCLUDED.active;

INSERT INTO admin_role_permissions (role_id, permission_id, created_at, created_by)
SELECT role.id, permission.id, NOW(), 'flyway'
FROM admin_roles role
JOIN admin_permissions permission ON TRUE
WHERE role.code IN ('SUPER_ADMIN', 'ADMIN')
ON CONFLICT (role_id, permission_id) DO NOTHING;

WITH role_scope_actions(role_code, scope, actions) AS (
    VALUES
        ('MANAGER', 'products', ARRAY['read','create','update','approve','export']::TEXT[]),
        ('MANAGER', 'categories', ARRAY['read','create','update','approve','export']::TEXT[]),
        ('MANAGER', 'media', ARRAY['read','create','update','approve','export']::TEXT[]),
        ('MANAGER', 'orders', ARRAY['read','create','update','approve','export']::TEXT[]),
        ('MANAGER', 'customers', ARRAY['read','create','update','approve','export']::TEXT[]),
        ('MANAGER', 'users', ARRAY['read','update']::TEXT[]),
        ('MANAGER', 'staff', ARRAY['read','update']::TEXT[]),
        ('MANAGER', 'roles', ARRAY['read']::TEXT[]),
        ('MANAGER', 'inventory', ARRAY['read','create','update','approve','export']::TEXT[]),
        ('MANAGER', 'discounts', ARRAY['read','create','update','approve','export']::TEXT[]),
        ('MANAGER', 'reviews', ARRAY['read','create','update','approve','export']::TEXT[]),
        ('MANAGER', 'reports', ARRAY['read','create','update','delete','approve','export']::TEXT[]),
        ('MANAGER', 'settings', ARRAY['read','update']::TEXT[]),
        ('MANAGER', 'integrations', ARRAY['read']::TEXT[]),
        ('MANAGER', 'audit_logs', ARRAY['read','export']::TEXT[]),

        ('CONTENT_MANAGER', 'products', ARRAY['read','create','update']::TEXT[]),
        ('CONTENT_MANAGER', 'categories', ARRAY['read','create','update']::TEXT[]),
        ('CONTENT_MANAGER', 'media', ARRAY['read','create','update']::TEXT[]),
        ('CONTENT_MANAGER', 'reviews', ARRAY['read','update']::TEXT[]),
        ('CONTENT_MANAGER', 'settings', ARRAY['read','update']::TEXT[]),
        ('CONTENT_MANAGER', 'reports', ARRAY['read']::TEXT[]),

        ('PRODUCT_MANAGER', 'products', ARRAY['read','create','update','approve','export']::TEXT[]),
        ('PRODUCT_MANAGER', 'categories', ARRAY['read','create','update']::TEXT[]),
        ('PRODUCT_MANAGER', 'media', ARRAY['read','create','update']::TEXT[]),
        ('PRODUCT_MANAGER', 'inventory', ARRAY['read','update']::TEXT[]),
        ('PRODUCT_MANAGER', 'discounts', ARRAY['read']::TEXT[]),
        ('PRODUCT_MANAGER', 'reviews', ARRAY['read','update']::TEXT[]),
        ('PRODUCT_MANAGER', 'reports', ARRAY['read','export']::TEXT[]),

        ('INVENTORY_MANAGER', 'inventory', ARRAY['read','create','update','delete','approve','export']::TEXT[]),
        ('INVENTORY_MANAGER', 'products', ARRAY['read','update']::TEXT[]),
        ('INVENTORY_MANAGER', 'orders', ARRAY['read','update']::TEXT[]),
        ('INVENTORY_MANAGER', 'reports', ARRAY['read','export']::TEXT[]),
        ('INVENTORY_MANAGER', 'audit_logs', ARRAY['read']::TEXT[]),

        ('ORDER_MANAGER', 'orders', ARRAY['read','create','update','approve','export']::TEXT[]),
        ('ORDER_MANAGER', 'customers', ARRAY['read','update']::TEXT[]),
        ('ORDER_MANAGER', 'reports', ARRAY['read','export']::TEXT[]),
        ('ORDER_MANAGER', 'audit_logs', ARRAY['read']::TEXT[]),

        ('SUPPORT_AGENT', 'customers', ARRAY['read','update']::TEXT[]),
        ('SUPPORT_AGENT', 'orders', ARRAY['read']::TEXT[]),
        ('SUPPORT_AGENT', 'reviews', ARRAY['read','update']::TEXT[]),
        ('SUPPORT_AGENT', 'reports', ARRAY['read']::TEXT[]),

        ('FINANCE', 'orders', ARRAY['read','export']::TEXT[]),
        ('FINANCE', 'discounts', ARRAY['read','create','update','approve','export']::TEXT[]),
        ('FINANCE', 'reports', ARRAY['read','create','update','delete','approve','export']::TEXT[]),
        ('FINANCE', 'settings', ARRAY['read','update']::TEXT[]),
        ('FINANCE', 'audit_logs', ARRAY['read','export']::TEXT[]),

        ('ANALYST', 'products', ARRAY['read']::TEXT[]),
        ('ANALYST', 'orders', ARRAY['read']::TEXT[]),
        ('ANALYST', 'customers', ARRAY['read']::TEXT[]),
        ('ANALYST', 'reports', ARRAY['read','export']::TEXT[]),
        ('ANALYST', 'audit_logs', ARRAY['read','export']::TEXT[]),

        ('STAFF', 'products', ARRAY['read']::TEXT[]),
        ('STAFF', 'orders', ARRAY['read']::TEXT[]),
        ('STAFF', 'customers', ARRAY['read']::TEXT[]),
        ('STAFF', 'inventory', ARRAY['read']::TEXT[]),
        ('STAFF', 'reports', ARRAY['read']::TEXT[]),

        ('WAREHOUSE_MANAGER', 'inventory', ARRAY['read','create','update','delete','approve','export']::TEXT[]),
        ('WAREHOUSE_MANAGER', 'products', ARRAY['read','update']::TEXT[]),
        ('WAREHOUSE_MANAGER', 'orders', ARRAY['read','update']::TEXT[]),
        ('WAREHOUSE_MANAGER', 'reports', ARRAY['read','export']::TEXT[]),

        ('VIEWER', 'inventory', ARRAY['read']::TEXT[]),
        ('VIEWER', 'products', ARRAY['read']::TEXT[]),
        ('VIEWER', 'orders', ARRAY['read']::TEXT[]),
        ('VIEWER', 'reports', ARRAY['read']::TEXT[])
)
INSERT INTO admin_role_permissions (role_id, permission_id, created_at, created_by)
SELECT role.id, permission.id, NOW(), 'flyway'
FROM role_scope_actions role_scope_action
JOIN admin_roles role ON role.code = role_scope_action.role_code
JOIN admin_permissions permission
    ON permission.scope = role_scope_action.scope
   AND permission.action = ANY(role_scope_action.actions)
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO admin_user_roles (user_id, role_id, created_at, created_by)
SELECT user_role.user_id, role.id, NOW(), 'flyway'
FROM user_roles user_role
JOIN admin_roles role ON role.code = UPPER(user_role.role)
WHERE UPPER(user_role.role) = 'ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

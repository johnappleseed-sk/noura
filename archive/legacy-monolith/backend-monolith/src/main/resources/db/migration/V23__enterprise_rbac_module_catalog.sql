ALTER TABLE admin_permissions
    ADD COLUMN IF NOT EXISTS module_group VARCHAR(80),
    ADD COLUMN IF NOT EXISTS display_order INTEGER,
    ADD COLUMN IF NOT EXISTS is_sensitive BOOLEAN;

ALTER TABLE admin_permissions
    ALTER COLUMN module_group SET DEFAULT 'governance',
    ALTER COLUMN display_order SET DEFAULT 1000,
    ALTER COLUMN is_sensitive SET DEFAULT FALSE;

UPDATE admin_permissions
SET module_group = COALESCE(module_group, 'governance'),
    display_order = COALESCE(display_order, 1000),
    is_sensitive = COALESCE(is_sensitive, FALSE);

ALTER TABLE admin_permissions
    ALTER COLUMN module_group SET NOT NULL,
    ALTER COLUMN display_order SET NOT NULL,
    ALTER COLUMN is_sensitive SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_admin_permissions_display_order_non_negative') THEN
        ALTER TABLE admin_permissions
            ADD CONSTRAINT ck_admin_permissions_display_order_non_negative CHECK (display_order >= 0);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_admin_permissions_scope_action') THEN
        ALTER TABLE admin_permissions
            ADD CONSTRAINT uk_admin_permissions_scope_action UNIQUE (scope, action);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_admin_permissions_group_order
    ON admin_permissions (module_group, display_order, scope, action);

CREATE INDEX IF NOT EXISTS idx_admin_permissions_sensitive
    ON admin_permissions (is_sensitive)
    WHERE is_sensitive = TRUE;

CREATE INDEX IF NOT EXISTS idx_admin_role_permissions_permission
    ON admin_role_permissions (permission_id);

WITH permission_seed(scope, action, label, description, module_group, display_order, is_sensitive) AS (
    VALUES
        ('dashboard', 'read_overview', 'Dashboard Overview', 'View executive dashboard overview.', 'core', 10, FALSE),
        ('dashboard', 'read_kpis', 'Dashboard KPIs', 'View KPI widgets and scorecards.', 'core', 11, FALSE),
        ('dashboard', 'configure_widgets', 'Dashboard Widgets', 'Configure dashboard widget layout and preferences.', 'core', 12, FALSE),

        ('catalog', 'read_products', 'Catalog Read', 'View catalog products and variants.', 'commerce', 20, FALSE),
        ('catalog', 'create_products', 'Catalog Create', 'Create products and variants.', 'commerce', 21, FALSE),
        ('catalog', 'update_products', 'Catalog Update', 'Update product attributes and merchandising metadata.', 'commerce', 22, FALSE),
        ('catalog', 'publish_products', 'Catalog Publish', 'Publish products to storefront channels.', 'commerce', 23, TRUE),

        ('inventory', 'read_stock', 'Inventory Read', 'View stock and warehouse inventory levels.', 'operations', 30, FALSE),
        ('inventory', 'update_stock', 'Inventory Update', 'Update stock levels and stock policy.', 'operations', 31, FALSE),
        ('inventory', 'adjust_stock', 'Inventory Adjust', 'Create stock adjustments and corrections.', 'operations', 32, TRUE),

        ('orders', 'read_orders', 'Orders Read', 'View order workflows and order history.', 'operations', 40, FALSE),
        ('orders', 'update_orders', 'Orders Update', 'Update order status and operational fields.', 'operations', 41, FALSE),
        ('orders', 'export_orders', 'Orders Export', 'Export order datasets.', 'operations', 42, FALSE),

        ('customers', 'read_customers', 'Customers Read', 'View customer profiles and segmentation.', 'commerce', 50, FALSE),
        ('customers', 'update_customers', 'Customers Update', 'Update customer operational metadata.', 'commerce', 51, FALSE),

        ('sales_promotions', 'read_promotions', 'Promotions Read', 'View promotion and coupon setup.', 'commerce', 60, FALSE),
        ('sales_promotions', 'create_promotions', 'Promotions Create', 'Create promotions and discount rules.', 'commerce', 61, FALSE),
        ('sales_promotions', 'approve_promotions', 'Promotions Approve', 'Approve promotion publishing.', 'commerce', 62, TRUE),

        ('finance', 'read_ledger', 'Finance Ledger', 'View finance ledgers and settlements.', 'finance', 70, TRUE),
        ('finance', 'reconcile_payments', 'Finance Reconcile', 'Reconcile payment transactions.', 'finance', 71, TRUE),
        ('finance', 'export_financials', 'Finance Export', 'Export finance and accounting datasets.', 'finance', 72, TRUE),

        ('marketing', 'manage_campaigns', 'Marketing Campaigns', 'Manage campaign setup and execution.', 'growth', 80, FALSE),
        ('marketing', 'manage_content', 'Marketing Content', 'Manage marketing copy and content modules.', 'growth', 81, FALSE),

        ('support', 'read_tickets', 'Support Read', 'View customer support tickets.', 'operations', 90, FALSE),
        ('support', 'resolve_tickets', 'Support Resolve', 'Resolve support tickets.', 'operations', 91, FALSE),

        ('vendors', 'read_vendors', 'Vendors Read', 'View vendor records and status.', 'supply_chain', 100, FALSE),
        ('vendors', 'manage_vendors', 'Vendors Manage', 'Manage vendor profiles and agreements.', 'supply_chain', 101, FALSE),

        ('analytics', 'read_analytics', 'Analytics Read', 'View analytics dashboards.', 'insights', 110, FALSE),
        ('analytics', 'export_analytics', 'Analytics Export', 'Export analytics data.', 'insights', 111, FALSE),

        ('shipping', 'read_shipments', 'Shipping Read', 'View shipment status and timelines.', 'operations', 120, FALSE),
        ('shipping', 'dispatch_shipments', 'Shipping Dispatch', 'Dispatch shipments to carriers.', 'operations', 121, FALSE),

        ('localization', 'read_locales', 'Localization Read', 'View locale and language setup.', 'commerce', 130, FALSE),
        ('localization', 'manage_translations', 'Localization Translations', 'Manage translations for commerce content.', 'commerce', 131, FALSE),

        ('procurement', 'read_purchase_orders', 'Procurement Read', 'View purchase orders.', 'supply_chain', 140, FALSE),
        ('procurement', 'approve_purchase_orders', 'Procurement Approve', 'Approve purchase orders.', 'supply_chain', 141, TRUE),

        ('users', 'read', 'Users Read', 'Read user accounts.', 'governance', 200, TRUE),
        ('users', 'update', 'Users Update', 'Update user accounts.', 'governance', 201, TRUE),
        ('roles', 'read', 'Roles Read', 'Read RBAC roles.', 'governance', 202, TRUE),
        ('roles', 'create', 'Roles Create', 'Create RBAC roles.', 'governance', 203, TRUE),
        ('roles', 'update', 'Roles Update', 'Update RBAC roles and permissions.', 'governance', 204, TRUE),
        ('roles', 'delete', 'Roles Delete', 'Deactivate RBAC roles.', 'governance', 205, TRUE),
        ('roles', 'approve', 'Roles Approve', 'Approve privileged RBAC changes.', 'governance', 206, TRUE),
        ('audit_logs', 'read', 'Audit Logs Read', 'Read audit log events.', 'governance', 207, TRUE),
        ('audit_logs', 'export', 'Audit Logs Export', 'Export audit log events.', 'governance', 208, TRUE)
)
INSERT INTO admin_permissions (scope, action, label, description, module_group, display_order, is_sensitive, created_at, created_by)
SELECT scope, action, label, description, module_group, display_order, is_sensitive, NOW(), 'flyway'
FROM permission_seed
ON CONFLICT (scope, action) DO UPDATE
SET
    label = EXCLUDED.label,
    description = EXCLUDED.description,
    module_group = EXCLUDED.module_group,
    display_order = EXCLUDED.display_order,
    is_sensitive = EXCLUDED.is_sensitive;

INSERT INTO admin_roles (code, label, description, is_system_role, assignable, active, created_at, created_by)
VALUES
    ('SUPER_ADMIN', 'Super Admin', 'Global unrestricted administrative role.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('ADMIN', 'Admin', 'Enterprise operations admin with broad governance rights.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('STORE_MANAGER', 'Store Manager', 'Owns store-level operations, catalog, orders, and fulfillment.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('PRODUCT_MANAGER', 'Product Manager', 'Owns product lifecycle and merchandising governance.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('INVENTORY_MANAGER', 'Inventory Manager', 'Owns inventory and warehouse operations.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('FINANCE_OFFICER', 'Finance Officer', 'Owns finance governance and reconciliation workflows.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('CUSTOMER_SUPPORT', 'Customer Support', 'Handles customer support workflows and escalations.', TRUE, TRUE, TRUE, NOW(), 'flyway'),
    ('MARKETING_MANAGER', 'Marketing Manager', 'Owns growth campaign and promotion operations.', TRUE, TRUE, TRUE, NOW(), 'flyway')
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

WITH role_grants(role_code, scope, action) AS (
    VALUES
        ('STORE_MANAGER', 'dashboard', 'read_overview'),
        ('STORE_MANAGER', 'dashboard', 'read_kpis'),
        ('STORE_MANAGER', 'catalog', 'read_products'),
        ('STORE_MANAGER', 'catalog', 'create_products'),
        ('STORE_MANAGER', 'catalog', 'update_products'),
        ('STORE_MANAGER', 'catalog', 'publish_products'),
        ('STORE_MANAGER', 'orders', 'read_orders'),
        ('STORE_MANAGER', 'orders', 'update_orders'),
        ('STORE_MANAGER', 'orders', 'export_orders'),
        ('STORE_MANAGER', 'inventory', 'read_stock'),
        ('STORE_MANAGER', 'inventory', 'update_stock'),
        ('STORE_MANAGER', 'customers', 'read_customers'),
        ('STORE_MANAGER', 'customers', 'update_customers'),
        ('STORE_MANAGER', 'sales_promotions', 'read_promotions'),
        ('STORE_MANAGER', 'sales_promotions', 'create_promotions'),
        ('STORE_MANAGER', 'sales_promotions', 'approve_promotions'),
        ('STORE_MANAGER', 'shipping', 'read_shipments'),
        ('STORE_MANAGER', 'shipping', 'dispatch_shipments'),
        ('STORE_MANAGER', 'analytics', 'read_analytics'),

        ('PRODUCT_MANAGER', 'catalog', 'read_products'),
        ('PRODUCT_MANAGER', 'catalog', 'create_products'),
        ('PRODUCT_MANAGER', 'catalog', 'update_products'),
        ('PRODUCT_MANAGER', 'catalog', 'publish_products'),
        ('PRODUCT_MANAGER', 'catalog', 'manage_media'),
        ('PRODUCT_MANAGER', 'marketing', 'manage_campaigns'),
        ('PRODUCT_MANAGER', 'marketing', 'manage_content'),
        ('PRODUCT_MANAGER', 'sales_promotions', 'read_promotions'),
        ('PRODUCT_MANAGER', 'sales_promotions', 'create_promotions'),
        ('PRODUCT_MANAGER', 'analytics', 'read_analytics'),

        ('INVENTORY_MANAGER', 'inventory', 'read_stock'),
        ('INVENTORY_MANAGER', 'inventory', 'update_stock'),
        ('INVENTORY_MANAGER', 'inventory', 'adjust_stock'),
        ('INVENTORY_MANAGER', 'orders', 'read_orders'),
        ('INVENTORY_MANAGER', 'orders', 'update_orders'),
        ('INVENTORY_MANAGER', 'shipping', 'read_shipments'),
        ('INVENTORY_MANAGER', 'shipping', 'dispatch_shipments'),
        ('INVENTORY_MANAGER', 'procurement', 'read_purchase_orders'),
        ('INVENTORY_MANAGER', 'procurement', 'approve_purchase_orders'),
        ('INVENTORY_MANAGER', 'vendors', 'read_vendors'),
        ('INVENTORY_MANAGER', 'vendors', 'manage_vendors'),

        ('FINANCE_OFFICER', 'finance', 'read_ledger'),
        ('FINANCE_OFFICER', 'finance', 'reconcile_payments'),
        ('FINANCE_OFFICER', 'finance', 'export_financials'),
        ('FINANCE_OFFICER', 'sales_promotions', 'read_promotions'),
        ('FINANCE_OFFICER', 'sales_promotions', 'approve_promotions'),
        ('FINANCE_OFFICER', 'orders', 'read_orders'),
        ('FINANCE_OFFICER', 'orders', 'export_orders'),
        ('FINANCE_OFFICER', 'analytics', 'read_analytics'),
        ('FINANCE_OFFICER', 'analytics', 'export_analytics'),
        ('FINANCE_OFFICER', 'audit_logs', 'read'),
        ('FINANCE_OFFICER', 'audit_logs', 'export'),

        ('CUSTOMER_SUPPORT', 'support', 'read_tickets'),
        ('CUSTOMER_SUPPORT', 'support', 'resolve_tickets'),
        ('CUSTOMER_SUPPORT', 'orders', 'read_orders'),
        ('CUSTOMER_SUPPORT', 'orders', 'update_orders'),
        ('CUSTOMER_SUPPORT', 'orders', 'approve_returns'),
        ('CUSTOMER_SUPPORT', 'customers', 'read_customers'),
        ('CUSTOMER_SUPPORT', 'customers', 'update_customers'),
        ('CUSTOMER_SUPPORT', 'shipping', 'read_shipments'),

        ('MARKETING_MANAGER', 'marketing', 'manage_campaigns'),
        ('MARKETING_MANAGER', 'marketing', 'manage_content'),
        ('MARKETING_MANAGER', 'catalog', 'read_products'),
        ('MARKETING_MANAGER', 'catalog', 'update_products'),
        ('MARKETING_MANAGER', 'catalog', 'publish_products'),
        ('MARKETING_MANAGER', 'sales_promotions', 'read_promotions'),
        ('MARKETING_MANAGER', 'sales_promotions', 'create_promotions'),
        ('MARKETING_MANAGER', 'sales_promotions', 'approve_promotions'),
        ('MARKETING_MANAGER', 'analytics', 'read_analytics'),
        ('MARKETING_MANAGER', 'analytics', 'export_analytics')
)
INSERT INTO admin_role_permissions (role_id, permission_id, created_at, created_by)
SELECT role.id, permission.id, NOW(), 'flyway'
FROM role_grants grant_row
JOIN admin_roles role ON role.code = grant_row.role_code
JOIN admin_permissions permission
    ON permission.scope = grant_row.scope
   AND permission.action = grant_row.action
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO admin_user_roles (user_id, role_id, created_at, created_by)
SELECT user_role.user_id, role.id, NOW(), 'flyway'
FROM user_roles user_role
JOIN admin_roles role ON role.code = 'ADMIN'
WHERE UPPER(user_role.role) = 'ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;

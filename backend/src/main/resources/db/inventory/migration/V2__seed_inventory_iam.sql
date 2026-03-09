INSERT IGNORE INTO iam_permissions (id, code, name, description, created_at)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'inventory:read', 'Inventory Read', 'Read inventory resources', CURRENT_TIMESTAMP(6)),
    ('10000000-0000-0000-0000-000000000002', 'inventory:write', 'Inventory Write', 'Create and update inventory resources', CURRENT_TIMESTAMP(6)),
    ('10000000-0000-0000-0000-000000000003', 'reports:read', 'Reports Read', 'Read reporting endpoints', CURRENT_TIMESTAMP(6)),
    ('10000000-0000-0000-0000-000000000004', 'audit:read', 'Audit Read', 'Read audit logs', CURRENT_TIMESTAMP(6)),
    ('10000000-0000-0000-0000-000000000005', 'webhooks:manage', 'Webhooks Manage', 'Manage webhook subscriptions', CURRENT_TIMESTAMP(6)),
    ('10000000-0000-0000-0000-000000000006', 'users:manage', 'Users Manage', 'Manage users and roles', CURRENT_TIMESTAMP(6));

UPDATE iam_permissions SET name = 'Inventory Read', description = 'Read inventory resources' WHERE code = 'inventory:read';
UPDATE iam_permissions SET name = 'Inventory Write', description = 'Create and update inventory resources' WHERE code = 'inventory:write';
UPDATE iam_permissions SET name = 'Reports Read', description = 'Read reporting endpoints' WHERE code = 'reports:read';
UPDATE iam_permissions SET name = 'Audit Read', description = 'Read audit logs' WHERE code = 'audit:read';
UPDATE iam_permissions SET name = 'Webhooks Manage', description = 'Manage webhook subscriptions' WHERE code = 'webhooks:manage';
UPDATE iam_permissions SET name = 'Users Manage', description = 'Manage users and roles' WHERE code = 'users:manage';

INSERT IGNORE INTO iam_roles (id, code, name, description, system_role, created_at)
VALUES
    ('20000000-0000-0000-0000-000000000001', 'ADMIN', 'Administrator', 'Full inventory platform access', TRUE, CURRENT_TIMESTAMP(6)),
    ('20000000-0000-0000-0000-000000000002', 'WAREHOUSE_MANAGER', 'Warehouse Manager', 'Warehouse operations access', TRUE, CURRENT_TIMESTAMP(6)),
    ('20000000-0000-0000-0000-000000000003', 'VIEWER', 'Viewer', 'Read-only inventory access', TRUE, CURRENT_TIMESTAMP(6));

UPDATE iam_roles SET name = 'Administrator', description = 'Full inventory platform access', system_role = TRUE WHERE code = 'ADMIN';
UPDATE iam_roles SET name = 'Warehouse Manager', description = 'Warehouse operations access', system_role = TRUE WHERE code = 'WAREHOUSE_MANAGER';
UPDATE iam_roles SET name = 'Viewer', description = 'Read-only inventory access', system_role = TRUE WHERE code = 'VIEWER';

INSERT IGNORE INTO iam_role_permissions (role_id, permission_id, granted_at)
SELECT role_id, permission_id, CURRENT_TIMESTAMP(6)
FROM (
    SELECT roles.id AS role_id, permissions.id AS permission_id
    FROM iam_roles roles
    JOIN iam_permissions permissions ON permissions.code = 'inventory:read'
    WHERE roles.code = 'ADMIN'
    UNION ALL
    SELECT roles.id, permissions.id
    FROM iam_roles roles
    JOIN iam_permissions permissions ON permissions.code = 'inventory:write'
    WHERE roles.code = 'ADMIN'
    UNION ALL
    SELECT roles.id, permissions.id
    FROM iam_roles roles
    JOIN iam_permissions permissions ON permissions.code = 'reports:read'
    WHERE roles.code = 'ADMIN'
    UNION ALL
    SELECT roles.id, permissions.id
    FROM iam_roles roles
    JOIN iam_permissions permissions ON permissions.code = 'audit:read'
    WHERE roles.code = 'ADMIN'
    UNION ALL
    SELECT roles.id, permissions.id
    FROM iam_roles roles
    JOIN iam_permissions permissions ON permissions.code = 'webhooks:manage'
    WHERE roles.code = 'ADMIN'
    UNION ALL
    SELECT roles.id, permissions.id
    FROM iam_roles roles
    JOIN iam_permissions permissions ON permissions.code = 'users:manage'
    WHERE roles.code = 'ADMIN'
    UNION ALL
    SELECT roles.id, permissions.id
    FROM iam_roles roles
    JOIN iam_permissions permissions ON permissions.code = 'inventory:read'
    WHERE roles.code = 'WAREHOUSE_MANAGER'
    UNION ALL
    SELECT roles.id, permissions.id
    FROM iam_roles roles
    JOIN iam_permissions permissions ON permissions.code = 'inventory:write'
    WHERE roles.code = 'WAREHOUSE_MANAGER'
    UNION ALL
    SELECT roles.id, permissions.id
    FROM iam_roles roles
    JOIN iam_permissions permissions ON permissions.code = 'reports:read'
    WHERE roles.code = 'WAREHOUSE_MANAGER'
    UNION ALL
    SELECT roles.id, permissions.id
    FROM iam_roles roles
    JOIN iam_permissions permissions ON permissions.code = 'inventory:read'
    WHERE roles.code = 'VIEWER'
    UNION ALL
    SELECT roles.id, permissions.id
    FROM iam_roles roles
    JOIN iam_permissions permissions ON permissions.code = 'reports:read'
    WHERE roles.code = 'VIEWER'
) seeded_permissions;

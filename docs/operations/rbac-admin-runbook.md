# RBAC Admin Runbook

## Purpose
Operational runbook for administering enterprise RBAC in production without breaking existing authentication or business workflows.

## Scope
- Admin UI: `/admin/governance/roles-permissions`
- Backend APIs: `/api/v1/admin/authorization/*`
- Persistence tables:
  - `admin_roles`
  - `admin_permissions`
  - `admin_role_permissions`
  - `admin_user_roles`
  - `admin_rbac_audit_logs`

## Required access
- Privileged role with RBAC governance permissions (`PERM_ROLES_*`, `PERM_USERS_*`, `PERM_AUDIT_LOGS_*`) or `ADMIN`/`SUPER_ADMIN`.
- PostgreSQL read access for operational verification queries.

## Environment baseline
1. Confirm backend is running and database migrations are applied.
2. Confirm RBAC reference data seed executed (permissions/roles/grants present).
3. Confirm admin UI can load:
   - role catalog
   - permission catalog
   - authorization matrix
   - audit log panel

## Change governance policy
1. For sensitive scope/actions (`is_sensitive=true`), require ticket + peer approval before applying changes.
2. Apply least-privilege grants first; avoid assigning `SUPER_ADMIN` for day-to-day operations.
3. Do not edit production grants directly in SQL except emergency recovery.
4. All policy changes must be made through RBAC APIs/UI so audit logging is preserved.

## Standard operations

### 1) Create custom role
1. Open `Roles & Permissions`.
2. Click `New Role`.
3. Set:
   - `code` in uppercase snake case (example: `FULFILLMENT_SUPERVISOR`)
   - `label`
   - `description`
   - `assignable=true`
   - `active=true`
4. Save metadata.
5. Assign permissions from grouped module cards.
6. Use module-level or global `Select All` only when justified.
7. Save permissions.

API equivalent:
```bash
curl -X POST "$API_BASE/api/v1/admin/authorization/roles" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "FULFILLMENT_SUPERVISOR",
    "label": "Fulfillment Supervisor",
    "description": "Manages fulfillment and shipment exception handling",
    "assignable": true,
    "active": true,
    "grants": {
      "orders": ["read_orders","update_orders"],
      "shipping": ["read_shipments","dispatch_shipments"]
    }
  }'
```

### 2) Update role permissions safely
1. Export or screenshot current grants before editing.
2. Modify grants in draft mode.
3. Save once and wait for success feedback.
4. Validate role capability impact via `/api/v1/admin/capabilities` on a test user.
5. Confirm an audit row exists.

### 3) Assign roles to users
1. In the user assignment panel, select target user.
2. Select active assignable roles only.
3. Save assignment replacement.
4. Ask user to refresh session/login so updated authorities are loaded.

Bulk assignment:
1. Select multiple users.
2. Choose role set.
3. Apply bulk update.
4. Validate `requestedUsers` and `updatedUsers` match expected count.

### 4) Deactivate role
1. Confirm role is not required by automation/service accounts.
2. Deactivate from UI (or `DELETE /roles/{roleId}`).
3. Verify role assignments are removed.
4. Verify no critical workflow loses required permissions.

## Module governance map
Use these modules as the primary ownership boundaries:
- Dashboard
- Catalog
- Inventory
- Orders
- Customers
- Sales & Promotions
- Finance
- Marketing
- Support
- Vendors
- Analytics
- Shipping
- Localization
- Procurement

## Audit and compliance operations

### Query recent RBAC mutations (API)
```bash
curl -X GET "$API_BASE/api/v1/admin/authorization/audit-logs?page=0&size=50&direction=desc" \
  -H "Authorization: Bearer $TOKEN"
```

### Export audit logs for review window
```bash
curl -L "$API_BASE/api/v1/admin/authorization/audit-logs/export?occurredFrom=2026-03-01T00:00:00Z&occurredTo=2026-03-31T23:59:59Z" \
  -H "Authorization: Bearer $TOKEN" \
  -o admin-rbac-audit-logs.csv
```

### Database verification queries
```sql
-- Active role count
SELECT COUNT(*) AS active_roles
FROM admin_roles
WHERE active = TRUE;

-- Sensitive grants by role
SELECT r.code AS role_code, p.scope, p.action
FROM admin_role_permissions rp
JOIN admin_roles r ON r.id = rp.role_id
JOIN admin_permissions p ON p.id = rp.permission_id
WHERE p.is_sensitive = TRUE
ORDER BY r.code, p.scope, p.action;

-- Latest RBAC audit events
SELECT occurred_at, action_type, entity_type, entity_id, actor_email, outcome, correlation_id, payload_hash
FROM admin_rbac_audit_logs
ORDER BY occurred_at DESC
LIMIT 50;
```

## Incident response (permission rollback)
1. Identify risky change from `admin_rbac_audit_logs` by `correlation_id`/`actor_email`/time window.
2. Temporarily remove affected user-role assignments if impact is active.
3. Restore previous role grants using:
   - previous CSV export, or
   - prior role snapshot from change ticket.
4. Re-validate key APIs with least-privileged test users.
5. Record incident summary and remediation ticket.

## Production guardrails
- Keep one emergency break-glass `SUPER_ADMIN` account with controlled credentials and MFA.
- Avoid shared admin accounts; each mutation must map to an individual actor.
- Rotate access and review role assignments at least monthly.
- Keep RBAC change reviews in release checklist for each deployment window.

## References
- RBAC API contracts: [docs/api/admin-authorization-matrix.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/admin-authorization-matrix.md)
- RBAC architecture: [docs/architecture/rbac-foundation.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/architecture/rbac-foundation.md)
- Enterprise RBAC feature: [docs/features/enterprise-rbac.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/features/enterprise-rbac.md)

# RBAC Persistent Architecture

## Goal
Provide an enterprise RBAC system where:
- authorization is persisted and managed through APIs
- runtime capability gating is derived from persisted role assignments
- frontend route/action guards consume backend-resolved capabilities

## Components
### Persistence
- `admin_roles`
- `admin_permissions`
- `admin_role_permissions`
- `admin_user_roles`
- `admin_bulk_user_role_views`
- `admin_rbac_audit_logs`
- Migration: `V20__admin_rbac_persistent_storage.sql`
- Migration: `V21__admin_rbac_audit_logs.sql`
- Migration: `V22__admin_rbac_audit_integrity_controls.sql`
- Migration: `V24__admin_rbac_bulk_assignment_saved_views.sql`

### Backend Services
- `AdminAuthorizationService`
  - capability resolution for authenticated sessions
  - matrix projection for governance UI
- `AdminRoleManagementService`
  - role CRUD
  - role grant replacement
  - role permission preset application
  - user role assignment replacement
  - bulk user role assignment preview
  - bulk user role assignment replacement
  - bulk assignment saved-view persistence (list/upsert/delete)
  - RBAC audit-log query projection
  - RBAC audit-log CSV export projection

### Controllers
- `AdminDashboardController`
  - `GET /api/v1/admin/capabilities`
  - `GET /api/v1/admin/authorization/matrix`
- `AdminAuthorizationController`
  - `/api/v1/admin/authorization/permissions`
  - `/api/v1/admin/authorization/permission-presets`
  - `/api/v1/admin/authorization/roles`
  - `/api/v1/admin/authorization/roles/{roleId}/permission-presets/{presetCode}`
  - `/api/v1/admin/authorization/users/{userId}/roles`
  - `/api/v1/admin/authorization/users/roles/bulk/preview`
  - `/api/v1/admin/authorization/users/roles/bulk`
  - `/api/v1/admin/authorization/users/roles/bulk/views`
  - `/api/v1/admin/authorization/audit-logs`
  - `/api/v1/admin/authorization/audit-logs/export`

### Security Integration
- `CustomUserDetailsService`
  - loads authorities from:
    - legacy `UserAccount.roles`
    - persisted `admin_user_roles`
  - derives permission authorities (`PERM_<SCOPE>_<ACTION>`) from role grants

### Frontend Integration
- `AuthProvider` consumes backend capabilities and resolved role set.
- `roles.js` maintains deterministic fallback capability mapping.
- `RolesPermissionsPage` provides role/governance operations.
- `RolesPermissionsPage` includes RBAC audit-log panel for recent governance mutations.
- `RolesPermissionsPage` includes RBAC audit filtering and CSV export actions.
- `RolesPermissionsPage` includes permission preset picker and bulk user-role assignment workflow with preview + CSV import/export.
- `RolesPermissionsPage` persists reusable bulk-assignment views through backend APIs (actor-scoped, not browser local storage).
- CSV import flow performs row-level validation and surfaces a structured import report before preview/apply.

## Request Lifecycle
1. User authenticates and authorities are built from legacy + persisted role assignments.
2. Admin app requests `/admin/capabilities`.
3. Backend resolves role set and capability map from persisted grants + defaults.
4. Router and navigation apply capability and role guards.
5. Governance page reads matrix and catalogs.
6. Bulk assignment workflow can save/reload actor-scoped view presets from persistent storage.
7. CSV import validates rows (`missing user_id`, duplicate rows, unknown users, invalid role codes) and returns in-page status details.
8. Bulk assignment workflow optionally previews role deltas and missing IDs before apply.
9. Role/assignment mutations (single and bulk) plus saved-view changes are persisted and recorded in RBAC audit logs with a deterministic `payload_hash`.
10. Audit rows are immutable at the database layer (update/delete blocked by trigger policy).
11. Updated assignments affect capability/authority resolution on subsequent requests.

## Design Decisions
- Keep compatibility with legacy roles while introducing persistent RBAC.
- Keep role capability model explicit for UI behavior.
- Use normalized scope-action permissions to prevent hard-coded per-route policy drift.
- Use permission-based method guards (`PERM_ROLES_*`, `PERM_USERS_*`, `PERM_AUDIT_LOGS_READ`) with admin-role fallback.

## Extension Path
1. Add permission simulation endpoint for “effective permissions for user”.
2. Add optimistic locking/version checks for concurrent role edits.
3. Add signed export manifests for RBAC audit CSV batches.
4. Expand endpoint-level `PERM_*` enforcement across non-RBAC admin modules.

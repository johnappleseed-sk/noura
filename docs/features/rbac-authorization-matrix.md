# RBAC Role and Permission Management

## Purpose
Deliver persistent enterprise RBAC management for admin operations:
- role catalog persistence
- permission catalog persistence
- permission preset templates
- role CRUD
- role-permission assignment
- user-role assignment
- bulk user-role assignment
- actor-scoped saved bulk assignment views
- RBAC mutation audit logging
- immutable audit-log integrity hashing
- capability projection for frontend route/action guards

## Architecture
- Persistence layer:
  - `admin_roles`
  - `admin_permissions`
  - `admin_role_permissions`
- `admin_user_roles`
- `admin_bulk_user_role_views`
- `admin_rbac_audit_logs`
- Backend modules:
  - `AdminAuthorizationService` + `AdminAuthorizationServiceImpl`
  - `AdminRoleManagementService` + `AdminRoleManagementServiceImpl`
  - `AdminDashboardController` (`/admin/capabilities`, `/admin/authorization/matrix`)
  - `AdminAuthorizationController` (`/admin/authorization/*` management endpoints)
- Frontend modules:
  - `frontend/admin-dashboard/src/shared/api/endpoints/adminAuthorizationApi.js`
  - `frontend/admin-dashboard/src/pages/RolesPermissionsPage.jsx`
  - `frontend/admin-dashboard/src/shared/auth/roles.js`
  - `frontend/admin-dashboard/src/features/auth/AuthProvider.jsx`

## API Endpoints
- Discovery:
  - `GET /api/v1/admin/capabilities`
  - `GET /api/v1/admin/authorization/matrix`
- Management:
  - `GET /api/v1/admin/authorization/permissions`
  - `GET /api/v1/admin/authorization/permission-presets`
  - `GET /api/v1/admin/authorization/roles`
  - `POST /api/v1/admin/authorization/roles`
  - `PATCH /api/v1/admin/authorization/roles/{roleId}`
  - `PUT /api/v1/admin/authorization/roles/{roleId}/permissions`
  - `PUT /api/v1/admin/authorization/roles/{roleId}/permission-presets/{presetCode}`
  - `DELETE /api/v1/admin/authorization/roles/{roleId}`
  - `GET /api/v1/admin/authorization/users/{userId}/roles`
  - `PUT /api/v1/admin/authorization/users/{userId}/roles`
  - `POST /api/v1/admin/authorization/users/roles/bulk/preview`
  - `PUT /api/v1/admin/authorization/users/roles/bulk`
  - `GET /api/v1/admin/authorization/users/roles/bulk/views`
  - `POST /api/v1/admin/authorization/users/roles/bulk/views`
  - `DELETE /api/v1/admin/authorization/users/roles/bulk/views/{viewId}`
  - `GET /api/v1/admin/authorization/audit-logs`
  - `GET /api/v1/admin/authorization/audit-logs/export`

Detailed request/response contracts are in [docs/api/admin-authorization-matrix.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/admin-authorization-matrix.md).

## Data Models
- Entities:
  - `AdminRole`
  - `AdminPermission`
  - `AdminRolePermission`
  - `AdminUserRole`
  - `AdminBulkUserRoleView`
  - `AdminRbacAuditLog`
- DTOs:
  - `AdminAuthorizationMatrixDto`
  - `AdminRolePermissionDto`
  - `AdminPermissionDto`
  - `AdminUserRoleAssignmentDto`
  - `AdminRoleCreateRequest`
  - `AdminRoleUpdateRequest`
  - `AdminRolePermissionUpdateRequest`
  - `AdminUserRoleAssignmentRequest`
  - `AdminBulkUserRoleAssignmentPreviewDto`
  - `AdminBulkUserRoleAssignmentPreviewItemDto`
  - `AdminBulkUserRoleViewDto`
  - `AdminBulkUserRoleViewUpsertRequest`
  - `AdminRbacAuditLogDto`

## Workflows
1. User authenticates.
2. Backend resolves effective role authorities from:
   - legacy `user_roles`
   - persisted `admin_user_roles`
3. Admin dashboard loads `/api/v1/admin/capabilities` for runtime route/action gating.
4. RBAC page loads:
   - matrix view (`/authorization/matrix`)
   - permissions (`/authorization/permissions`)
   - roles (`/authorization/roles`)
5. Admin executes management actions:
   - create/update/deactivate role
   - replace role grants
   - apply policy preset onto role grants
   - replace user role assignments
   - preview role deltas for multiple users in a batch
   - replace assignments for multiple users in a batch
  - save/import/export reusable bulk assignment selections in UI workflows
  - persist and reuse saved bulk-assignment views from server storage (scoped per admin actor)
  - import CSV with row-level validation report (`ok`/`warning`/`error`) before preview/apply
6. Backend writes immutable RBAC audit events for each role/assignment mutation.
7. Updated assignments immediately affect authority/capability resolution on next authenticated request.
8. Audit logs can be filtered by action/entity/text/date range and exported to CSV for compliance workflows.

## Permissions Required
- Discovery:
  - `/admin/capabilities`: authenticated users
  - `/admin/authorization/matrix`: `PERM_ROLES_READ` or `ADMIN`/`SUPER_ADMIN`
- Management:
  - read role catalogs: `PERM_ROLES_READ` or `ADMIN`/`SUPER_ADMIN`
  - create role: `PERM_ROLES_CREATE` or `ADMIN`/`SUPER_ADMIN`
  - update role/grants: `PERM_ROLES_UPDATE` (or `PERM_ROLES_APPROVE` for grants) or `ADMIN`/`SUPER_ADMIN`
  - deactivate role: `PERM_ROLES_DELETE` or `ADMIN`/`SUPER_ADMIN`
  - user role reads: `PERM_ROLES_READ` + `PERM_USERS_READ` or `ADMIN`/`SUPER_ADMIN`
  - user role writes: (`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) + `PERM_USERS_UPDATE` or `ADMIN`/`SUPER_ADMIN`
  - bulk preview writes: (`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) + `PERM_USERS_UPDATE` or `ADMIN`/`SUPER_ADMIN`
  - bulk user-role writes: (`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) + `PERM_USERS_UPDATE` or `ADMIN`/`SUPER_ADMIN`
  - bulk saved-view read/write/delete: (`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) + `PERM_USERS_UPDATE` or `ADMIN`/`SUPER_ADMIN`
  - RBAC audit logs: `PERM_AUDIT_LOGS_READ` (or `PERM_ROLES_READ`) or `ADMIN`/`SUPER_ADMIN`
  - RBAC audit-log export: `PERM_AUDIT_LOGS_EXPORT` or `ADMIN`/`SUPER_ADMIN`

## Usage Example
1. Open `/admin/governance/roles-permissions`.
2. Create role `SUPPORT_LEAD`.
3. Assign grants under `orders` and `customers`.
4. Assign role to a target user from the user-role assignment section.
5. Re-login as that user and verify capability-gated navigation updates.

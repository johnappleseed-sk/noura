# Roles & Permissions Admin Page

## Overview
Page path: `/admin/governance/roles-permissions`

Purpose:
- Manage enterprise RBAC policy using persistent backend records.
- Support role CRUD and role/user assignment workflows in one governance screen.
- Provide matrix diagnostics for scope-action visibility by role.

## State Management
- Local React state (`useState`) for:
  - loading/error/success feedback
  - matrix + role + permission + user + audit-log catalogs
  - permission preset catalog
  - role editor form + permission draft
  - selected permission preset
  - selected user assignment draft
  - bulk user selection + bulk role draft
  - bulk assignment preview payload
  - saved bulk views (persisted in backend per admin actor)
  - CSV import validation report (row-level status/messages)
  - filter controls (query, scope, active-only)
  - audit-log filter controls (action/entity/query/from/to) and CSV export state
- Derived data via `useMemo`:
  - selected role/user views
  - filtered role/scope matrix projections
  - assignable role set for user assignment

## Data Flow
1. On mount:
   - `getAdminAuthorizationMatrix()`
   - `listAdminPermissions()`
   - `listAdminPermissionPresets()`
   - `listAdminRoles()`
   - `listAdminUsers()`
   - `listAdminBulkUserRoleViews()`
   - `listAdminAuthorizationAuditLogs()`
2. Selecting a user triggers `getAdminUserRoleAssignments(userId)`.
3. Save actions:
   - role metadata: `createAdminRole` or `updateAdminRole`
   - role lifecycle: `deactivateAdminRole`
   - role grants: `replaceAdminRolePermissions`
   - user assignments: `replaceAdminUserRoleAssignments`
4. Mutation success paths refresh RBAC audit logs.
5. UI refreshes role/matrix data after successful mutations.
6. Audit section supports date-bound filtering and CSV export through `exportAdminAuthorizationAuditLogsCsv`.
7. Permission workspace supports preset application (`applyAdminRolePermissionPreset`) and updates the editable draft in-place.
8. User assignment section supports bulk role replacement (`bulkReplaceAdminUserRoleAssignments`).
9. Bulk section supports change preview (`previewBulkAdminUserRoleAssignments`) before apply.
10. Bulk section supports server-backed saved views (`upsertAdminBulkUserRoleView`, `deleteAdminBulkUserRoleView`) with actor scoping.
11. Bulk section supports CSV export/import for selected user IDs and role sets.
12. CSV import performs row-level validation (`missing user_id`, duplicate user, unknown user, invalid role codes) and renders a structured result report in-page.

## UX Behaviors
- Loading: full-page spinner.
- Error: inline alert + toast feedback.
- Success: toast feedback for create/update/deactivate/assignment operations.
- Empty:
  - no roles for filter criteria
  - no selected role for permission editor
  - no selected user for assignment section
  - no matching role/scope pairs for matrix preview
  - no RBAC audit events recorded
- Filtering:
  - free-text search across role metadata/capabilities
  - optional scope filter
  - active-runtime-only toggle
- Duplicate submission safety:
  - save buttons disabled while async mutations are in flight

## Permission Guard
- Router guard:
  - capability: `governance.rbac`
  - no hard-coded admin role restriction (permission-derived capability can grant access)

## Workflows
### Role CRUD
1. Click `New role`.
2. Fill code, label, description, lifecycle flags.
3. Save via `Create role` or `Update role`.
4. Deactivation uses confirmation dialog and removes role assignments.

### Permission Assignment
1. Select role.
2. Toggle scope/action checkboxes.
3. Save with `Save permissions` to replace role grants.

### Permission Presets
1. Select role.
2. Choose preset from `Preset` picker.
3. Click `Apply preset` to replace grants from selected policy template.

### User Role Assignment
1. Select user.
2. Toggle assignable active roles.
3. Save with `Save assignments`.

### Bulk User Role Assignment
1. Filter and select multiple users.
2. Toggle assignable active roles in bulk role checklist.
3. Optional: save a reusable view (query + user selection + role draft) to backend persistent storage.
4. Optional: export selected users/roles to CSV, or import CSV (`user_id` required, `role_codes` optional).
5. Review CSV import report (row status + warnings/errors).
6. Preview with `Preview changes` to inspect adds/removals and missing IDs.
7. Save with `Apply bulk assignments` after preview validation.

### RBAC Audit Logs
1. Page loads latest RBAC governance events.
2. Successful role or assignment mutations trigger log refresh.
3. Admin can filter by action/entity/text/date range and refresh the panel.
4. Admin can export the filtered set as CSV.
5. Audit table renders `payloadHash` to support integrity review.

## Related Files
- Route: `frontend/admin-dashboard/src/app/router.jsx`
- Navigation: `frontend/admin-dashboard/src/app/navigation.js`
- Capability constants/fallback: `frontend/admin-dashboard/src/shared/auth/roles.js`
- Page: `frontend/admin-dashboard/src/pages/RolesPermissionsPage.jsx`
- API client: `frontend/admin-dashboard/src/shared/api/endpoints/adminAuthorizationApi.js`

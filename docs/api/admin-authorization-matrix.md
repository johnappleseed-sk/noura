# Admin Authorization API

## Overview
RBAC endpoints under `/api/v1/admin` for:
- capability discovery
- matrix inspection
- role CRUD
- permission assignment
- user-role assignment
- bulk assignment saved-view persistence
- RBAC audit-log retrieval

All endpoints use standard API envelope:
```json
{
  "success": true,
  "message": "Operation message",
  "data": {},
  "path": "/api/v1/..."
}
```

## Authentication
- `Bearer` token required for all endpoints.
- `/api/v1/admin/capabilities`: any authenticated user.
- `/api/v1/admin/authorization/matrix`: `PERM_ROLES_READ` or `ADMIN`/`SUPER_ADMIN`.
- Role/permission management endpoints are permission-aware with `ADMIN`/`SUPER_ADMIN` fallback:
  - catalog read: `PERM_ROLES_READ`
  - permission preset catalog read: `PERM_ROLES_READ`
  - role create: `PERM_ROLES_CREATE`
  - role update/grant replace/preset apply: `PERM_ROLES_UPDATE` (or `PERM_ROLES_APPROVE` for grant replacement)
  - role deactivate: `PERM_ROLES_DELETE`
  - user-role read: `PERM_ROLES_READ` + `PERM_USERS_READ`
  - user-role replace (single + bulk): (`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) + `PERM_USERS_UPDATE`
  - user-role bulk preview: (`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) + `PERM_USERS_UPDATE`
  - user-role bulk saved views (list/upsert/delete): (`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) + `PERM_USERS_UPDATE`
  - RBAC audit logs: `PERM_AUDIT_LOGS_READ` or `PERM_ROLES_READ`
  - RBAC audit-log export: `PERM_AUDIT_LOGS_EXPORT`

## Capability Discovery
### `GET /api/v1/admin/capabilities`

Response `data`:
```json
{
  "roles": ["ADMIN", "ORDER_MANAGER"],
  "capabilities": {
    "overview.dashboard": true,
    "commerce.orders": true,
    "governance.rbac": true
  }
}
```

Example:
```bash
curl -X GET "http://localhost:8080/api/v1/admin/capabilities" \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

## Matrix Projection
### `GET /api/v1/admin/authorization/matrix`

Response `data`:
```json
{
  "version": "rbac-matrix-v2-persistent",
  "actionCatalog": ["read", "create", "update", "delete", "approve", "export"],
  "scopes": [
    {
      "scope": "products",
      "label": "Products",
      "description": "Catalog products, variants, merchandising flags, and SEO fields.",
      "supportedActions": ["read", "create", "update", "delete", "approve", "export"]
    }
  ],
  "roles": [
    {
      "id": "fdd90e7d-c722-44a8-9834-4ef78d4ec3eb",
      "role": "ADMIN",
      "label": "Admin",
      "description": "Primary operations administrator for commerce and warehouse modules.",
      "systemRole": true,
      "assignable": true,
      "activeInRuntime": true,
      "assignedUsers": 5,
      "grants": {
        "products": ["read", "create", "update", "delete", "approve", "export"]
      },
      "capabilities": ["overview.dashboard", "commerce.catalog", "governance.rbac"]
    }
  ]
}
```

## Permission Catalog
### `GET /api/v1/admin/authorization/permissions`

Response `data`:
```json
[
  {
    "id": "2f65d52f-4444-4ec9-a636-c7e955fbe219",
    "scope": "products",
    "action": "read",
    "label": "Products Read",
    "description": "Allows read access in products scope."
  }
]
```

## Role Catalog
### `GET /api/v1/admin/authorization/roles`

Response `data`: array of `AdminRolePermissionDto` entries.

## Create Role
### `POST /api/v1/admin/authorization/roles`

Request body:
```json
{
  "code": "SUPPORT_LEAD",
  "label": "Support Lead",
  "description": "Escalation and support governance role",
  "assignable": true,
  "active": true,
  "grants": {
    "orders": ["read", "update", "approve"],
    "customers": ["read", "update"]
  }
}
```

## Permission Presets
### `GET /api/v1/admin/authorization/permission-presets`

Response `data`:
```json
[
  {
    "code": "ORDER_MANAGER",
    "label": "Order Manager",
    "description": "Order operations preset",
    "moduleCount": 2,
    "permissionCount": 6,
    "grants": {
      "orders": ["read", "update", "approve"]
    }
  }
]
```

Response `data`: created `AdminRolePermissionDto`.

## Update Role Metadata
### `PATCH /api/v1/admin/authorization/roles/{roleId}`

Request body:
```json
{
  "label": "Support Lead",
  "description": "Updated description",
  "assignable": true,
  "active": true
}
```

Response `data`: updated `AdminRolePermissionDto`.

## Replace Role Permissions
### `PUT /api/v1/admin/authorization/roles/{roleId}/permissions`

Request body:
```json
{
  "grants": {
    "orders": ["read", "update", "approve"],
    "customers": ["read", "update"]
  }
}
```

Response `data`: updated `AdminRolePermissionDto`.

## Apply Permission Preset
### `PUT /api/v1/admin/authorization/roles/{roleId}/permission-presets/{presetCode}`

Request body: none.

Behavior:
- resolves an active system-role preset by `presetCode`
- replaces target role grants with preset grants
- records RBAC audit event `ROLE_PERMISSION_PRESET_APPLIED`

Response `data`: updated `AdminRolePermissionDto`.

## Deactivate Role
### `DELETE /api/v1/admin/authorization/roles/{roleId}`

Request body: none.

Behavior:
- sets role `active=false`
- removes `admin_user_roles` assignments for that role

## Read User Role Assignments
### `GET /api/v1/admin/authorization/users/{userId}/roles`

Response `data`:
```json
{
  "userId": "f93ca61f-16f0-4897-a87b-963f7c6ef4e7",
  "email": "ops@noura.test",
  "fullName": "Ops User",
  "adminRoleCodes": ["ORDER_MANAGER", "SUPPORT_AGENT"],
  "platformRoles": ["ADMIN"]
}
```

## Replace User Role Assignments
### `PUT /api/v1/admin/authorization/users/{userId}/roles`

Request body:
```json
{
  "roleCodes": ["ORDER_MANAGER", "SUPPORT_AGENT"]
}
```

Response `data`: updated `AdminUserRoleAssignmentDto`.

## Preview Bulk User Role Assignments
### `POST /api/v1/admin/authorization/users/roles/bulk/preview`

Request body:
```json
{
  "userIds": [
    "f93ca61f-16f0-4897-a87b-963f7c6ef4e7",
    "4bb5f1a6-bf9b-4c6d-a993-e08ea7e515d2"
  ],
  "roleCodes": ["ORDER_MANAGER", "SUPPORT_AGENT"]
}
```

Behavior:
- validates all `roleCodes` against assignable + active admin roles
- resolves existing user records by ID
- returns missing IDs plus per-user add/remove role deltas

Response `data`:
```json
{
  "requestedUsers": 2,
  "resolvableUsers": 1,
  "missingUsers": 1,
  "changedUsers": 1,
  "missingUserIds": ["4bb5f1a6-bf9b-4c6d-a993-e08ea7e515d2"],
  "items": [
    {
      "userId": "f93ca61f-16f0-4897-a87b-963f7c6ef4e7",
      "email": "ops@noura.test",
      "fullName": "Ops User",
      "currentRoleCodes": ["SUPPORT_AGENT"],
      "proposedRoleCodes": ["ORDER_MANAGER", "SUPPORT_AGENT"],
      "rolesToAdd": ["ORDER_MANAGER"],
      "rolesToRemove": [],
      "changed": true
    }
  ]
}
```

## Bulk Replace User Role Assignments
### `PUT /api/v1/admin/authorization/users/roles/bulk`

Request body:
```json
{
  "userIds": [
    "f93ca61f-16f0-4897-a87b-963f7c6ef4e7",
    "4bb5f1a6-bf9b-4c6d-a993-e08ea7e515d2"
  ],
  "roleCodes": ["ORDER_MANAGER", "SUPPORT_AGENT"]
}
```

## Saved Bulk Assignment Views
### `GET /api/v1/admin/authorization/users/roles/bulk/views`

Response `data`:
```json
[
  {
    "id": "f6a0f41f-bf3e-4a9d-bf40-1f0b315f3bd3",
    "name": "Ops leads - APAC",
    "query": "ops",
    "userIds": [
      "f93ca61f-16f0-4897-a87b-963f7c6ef4e7"
    ],
    "roleCodes": ["ORDER_MANAGER", "SUPPORT_AGENT"],
    "updatedAt": "2026-03-13T11:00:00Z"
  }
]
```

### `POST /api/v1/admin/authorization/users/roles/bulk/views`

Request body:
```json
{
  "name": "Ops leads - APAC",
  "query": "ops",
  "userIds": [
    "f93ca61f-16f0-4897-a87b-963f7c6ef4e7"
  ],
  "roleCodes": ["ORDER_MANAGER", "SUPPORT_AGENT"]
}
```

Behavior:
- upserts by `(owner_user_id, lower(name))`
- normalizes and deduplicates user IDs
- validates role codes against active + assignable roles
- records RBAC audit event `USER_ROLE_BULK_VIEW_UPSERTED`

Response `data`: persisted `AdminBulkUserRoleViewDto`.

### `DELETE /api/v1/admin/authorization/users/roles/bulk/views/{viewId}`

Behavior:
- deletes only actor-owned saved view
- returns `404 BULK_VIEW_NOT_FOUND` when not found for actor
- records RBAC audit event `USER_ROLE_BULK_VIEW_DELETED`

Response `data`: `null`

Response `data`:
```json
{
  "requestedUsers": 2,
  "updatedUsers": 2,
  "assignments": [
    {
      "userId": "f93ca61f-16f0-4897-a87b-963f7c6ef4e7",
      "email": "ops@noura.test",
      "fullName": "Ops User",
      "adminRoleCodes": ["ORDER_MANAGER", "SUPPORT_AGENT"],
      "platformRoles": ["ADMIN"]
    }
  ]
}
```

## RBAC Audit Logs
### `GET /api/v1/admin/authorization/audit-logs`

Query params:
- `actionType` (optional)
- `entityType` (optional)
- `outcome` (optional)
- `query` (optional)
- `errorsOnly` (optional boolean)
- `occurredFrom` (optional, ISO-8601 timestamp inclusive lower bound)
- `occurredTo` (optional, ISO-8601 timestamp inclusive upper bound)
- `page` (default `0`)
- `size` (default `20`, max `100`)
- `sortBy` (default `occurredAt`)
- `direction` (default `desc`)

Response `data` (`PageResponse<AdminRbacAuditLogDto>`):
```json
{
  "content": [
    {
      "id": "1021498d-f6ec-48d8-9ce8-fd9003c75a3f",
      "actionType": "ROLE_PERMISSIONS_REPLACED",
      "entityType": "ROLE",
      "entityId": "fdd90e7d-c722-44a8-9834-4ef78d4ec3eb",
      "actorEmail": "admin@noura.test",
      "actorUserId": "67a2d0a8-d353-49ef-afeb-245a2fdcd91d",
      "outcome": "SUCCESS",
      "correlationId": "f262f4ef-d186-4f7b-b540-20194e9dbe9f",
      "payloadHash": "8f47f5048a916f8964f111e57b5f98395de036d26e9f5b204ee40b7446a4ca33",
      "detailsJson": "{\"grants\":{\"orders\":[\"read\",\"update\"]}}",
      "occurredAt": "2026-03-13T10:10:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

## RBAC Audit Logs CSV Export
### `GET /api/v1/admin/authorization/audit-logs/export`

Query params:
- `actionType` (optional)
- `entityType` (optional)
- `query` (optional)
- `occurredFrom` (optional, ISO-8601)
- `occurredTo` (optional, ISO-8601)

Auth:
- `PERM_AUDIT_LOGS_EXPORT` or `ADMIN`/`SUPER_ADMIN`

Response:
- Content-Type: `text/csv`
- Content-Disposition filename: `admin-rbac-audit-logs.csv`
- Column set:
  - `occurred_at`
  - `action_type`
  - `entity_type`
  - `entity_id`
  - `actor_email`
  - `actor_user_id`
  - `outcome`
  - `correlation_id`
  - `payload_hash`
  - `details_json`

## Error Responses
- `400`:
  - `ROLE_EXISTS`
  - `ROLE_CODE_REQUIRED`
  - `ROLE_LABEL_REQUIRED`
  - `PERMISSION_NOT_FOUND`
  - `ROLE_INACTIVE`
  - `ROLE_NOT_ASSIGNABLE`
  - `BULK_VIEW_NAME_REQUIRED`
  - `BULK_VIEW_NAME_INVALID`
  - `BULK_VIEW_QUERY_INVALID`
- `401 AUTH_REQUIRED`
- `403 ACCESS_DENIED`
- `404 ROLE_NOT_FOUND` / `USER_NOT_FOUND` / `BULK_VIEW_NOT_FOUND`

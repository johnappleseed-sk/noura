# Backend API

## Admin Authorization Endpoints
Base prefix: `/api/v1`

### Resolve admin capabilities
`GET /admin/capabilities`

Auth:
- Any authenticated session

Response `data` example:
```json
{
  "roles": ["ADMIN"],
  "capabilities": {
    "overview.dashboard": true,
    "commerce.catalog": true,
    "governance.rbac": true
  }
}
```

### Resolve admin role-permission matrix
`GET /admin/authorization/matrix`

Auth:
- `PERM_ROLES_READ` or `ADMIN`/`SUPER_ADMIN`

Response `data` includes:
- `version`
- `actionCatalog`
- `scopes`
- `roles` (with grants and capabilities)

### List permission catalog
`GET /admin/authorization/permissions`

Auth:
- `PERM_ROLES_READ` or `ADMIN`/`SUPER_ADMIN`

### List permission presets
`GET /admin/authorization/permission-presets`

Auth:
- `PERM_ROLES_READ` or `ADMIN`/`SUPER_ADMIN`

### List roles
`GET /admin/authorization/roles`

Auth:
- `PERM_ROLES_READ` or `ADMIN`/`SUPER_ADMIN`

### Create role
`POST /admin/authorization/roles`

Auth:
- `PERM_ROLES_CREATE` or `ADMIN`/`SUPER_ADMIN`

### Update role metadata
`PATCH /admin/authorization/roles/{roleId}`

Auth:
- `PERM_ROLES_UPDATE` or `ADMIN`/`SUPER_ADMIN`

### Replace role permissions
`PUT /admin/authorization/roles/{roleId}/permissions`

Auth:
- (`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) or `ADMIN`/`SUPER_ADMIN`

### Apply role permission preset
`PUT /admin/authorization/roles/{roleId}/permission-presets/{presetCode}`

Auth:
- (`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) or `ADMIN`/`SUPER_ADMIN`

### Deactivate role
`DELETE /admin/authorization/roles/{roleId}`

Auth:
- `PERM_ROLES_DELETE` or `ADMIN`/`SUPER_ADMIN`

### Read user role assignments
`GET /admin/authorization/users/{userId}/roles`

Auth:
- (`PERM_ROLES_READ` and `PERM_USERS_READ`) or `ADMIN`/`SUPER_ADMIN`

### Replace user role assignments
`PUT /admin/authorization/users/{userId}/roles`

Auth:
- ((`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) and `PERM_USERS_UPDATE`) or `ADMIN`/`SUPER_ADMIN`

### Preview bulk user role assignments
`POST /admin/authorization/users/roles/bulk/preview`

Auth:
- ((`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) and `PERM_USERS_UPDATE`) or `ADMIN`/`SUPER_ADMIN`

Response highlights:
- `requestedUsers`
- `resolvableUsers`
- `missingUsers`
- `changedUsers`
- `missingUserIds[]`
- `items[]` with `currentRoleCodes`, `proposedRoleCodes`, `rolesToAdd`, `rolesToRemove`

### Bulk replace user role assignments
`PUT /admin/authorization/users/roles/bulk`

Auth:
- ((`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) and `PERM_USERS_UPDATE`) or `ADMIN`/`SUPER_ADMIN`

### List saved bulk assignment views
`GET /admin/authorization/users/roles/bulk/views`

Auth:
- ((`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) and `PERM_USERS_UPDATE`) or `ADMIN`/`SUPER_ADMIN`

Response `data` fields:
- `id`
- `name`
- `query`
- `userIds[]`
- `roleCodes[]`
- `updatedAt`

### Upsert saved bulk assignment view
`POST /admin/authorization/users/roles/bulk/views`

Auth:
- ((`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) and `PERM_USERS_UPDATE`) or `ADMIN`/`SUPER_ADMIN`

Request `data` fields:
- `name` (required, max 120)
- `query` (optional, max 255)
- `userIds[]` (optional, max 200)
- `roleCodes[]` (optional, max 30, validated as active assignable roles)

### Delete saved bulk assignment view
`DELETE /admin/authorization/users/roles/bulk/views/{viewId}`

Auth:
- ((`PERM_ROLES_UPDATE` or `PERM_ROLES_APPROVE`) and `PERM_USERS_UPDATE`) or `ADMIN`/`SUPER_ADMIN`

Notes:
- View ownership is actor-scoped.
- Returns `404 BULK_VIEW_NOT_FOUND` for unknown or non-owned IDs.

### Read RBAC audit logs
`GET /admin/authorization/audit-logs`

Auth:
- `PERM_AUDIT_LOGS_READ` or `PERM_ROLES_READ` or `ADMIN`/`SUPER_ADMIN`

Query params:
- `actionType` (optional)
- `entityType` (optional)
- `outcome` (optional)
- `query` (optional)
- `errorsOnly` (optional boolean)
- `occurredFrom` (optional ISO-8601 inclusive lower bound)
- `occurredTo` (optional ISO-8601 inclusive upper bound)
- `page`/`size`/`sortBy`/`direction`

### Export RBAC audit logs
`GET /admin/authorization/audit-logs/export`

Auth:
- `PERM_AUDIT_LOGS_EXPORT` or `ADMIN`/`SUPER_ADMIN`

Response:
- `text/csv` attachment (`admin-rbac-audit-logs.csv`)
- Includes audit columns and `payload_hash` integrity value

See detailed endpoint contract:
- [docs/api/admin-authorization-matrix.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/admin-authorization-matrix.md)

## Product Enrichment Endpoints
Base prefix: `/api/v1`

### Search existing products
`GET /products/search?q={query}`

Auth:
- `ADMIN`

Response `data` example:
```json
[
  {
    "id": "9a95dcad-8498-4e04-8b68-94cd12b1cd50",
    "name": "Noura Pro Headphones",
    "category": "Electronics",
    "description_missing": true,
    "barcode_missing": false,
    "qr_missing": true,
    "mirror_status": "UNMAPPED"
  }
]
```

Notes:
- Query supports product UUID exact match, name contains, and variant SKU contains.
- Results are deduplicated and capped.

### Get product details
`GET /products/{id}`

Response includes enrichment fields:
- `description`
- `targetAudience`
- `barcode`
- `qrCode`

### Generate all missing fields
`POST /products/{id}/generate-missing`

Auth:
- `ADMIN`

Behavior:
- Generates only missing values for description/barcode/qr.
- Persists updates in commerce product.
- Queues mirror sync job for changed fields.

Response `data` example:
```json
{
  "id": "9a95dcad-8498-4e04-8b68-94cd12b1cd50",
  "product_name": "Noura Pro Headphones",
  "description": "...",
  "barcode": "1234567890128",
  "qr_code": "https://store.example.com/products/9a95dcad-8498-4e04-8b68-94cd12b1cd50",
  "barcode_image_url": "/api/v1/products/9a95dcad-8498-4e04-8b68-94cd12b1cd50/barcode-image",
  "qr_image_url": "/api/v1/products/9a95dcad-8498-4e04-8b68-94cd12b1cd50/qr-image",
  "description_generated": true,
  "barcode_generated": true,
  "qr_generated": true,
  "mirror_status": "PENDING",
  "mirror_warning": null
}
```

### Generate description only
`POST /products/{id}/generate-description`

Auth:
- `ADMIN`

Behavior:
- Generates description only when missing.

### Generate barcode only
`POST /products/{id}/generate-barcode`

Auth:
- `ADMIN`

Behavior:
- Generates unique EAN-13 barcode only when missing.

### Generate QR only
`POST /products/{id}/generate-qr`

Auth:
- `ADMIN`

Behavior:
- Generates QR payload only when missing.

### Render barcode image
`GET /products/{id}/barcode-image`

Auth:
- `ADMIN`

Response:
- `image/png`
- `404` when barcode is missing.

### Render QR image
`GET /products/{id}/qr-image`

Auth:
- `ADMIN`

Response:
- `image/png`
- `404` when QR value is missing.

## Error Cases
Common errors for enrichment endpoints:
- Product not found/inactive
- Access denied for non-admin role
- Barcode generation failure after collision retries
- External LLM failure (auto-fallback to template description)
- Mirror mapping missing (`mirror_status=BLOCKED_MAPPING` + warning)

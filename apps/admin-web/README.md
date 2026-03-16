# Noura Admin Dashboard

React admin dashboard for the Noura inventory service.

## Feature highlights
- Capability-gated admin routing/navigation
- Commerce catalog, orders, pricing, returns, analytics, and governance workspaces
- Roles & permissions governance page (`/admin/governance/roles-permissions`) backed by persistent RBAC APIs:
  - role CRUD
  - role-permission assignment
  - user-role assignment
  - matrix diagnostics
  - RBAC mutation audit-log visibility

## Docs
- RBAC feature: `/docs/features/rbac-authorization-matrix.md`
- RBAC API: `/docs/api/admin-authorization-matrix.md`
- Roles page behavior: `/docs/frontend/roles-permissions-page.md`
- Onboarding: `/docs/onboarding.md`

## Local development

```bash
npm install
npm run dev
```

By default the dashboard calls `http://localhost:8080` (monolithic backend).

Override the API base URL:

```bash
export VITE_API_BASE_URL="http://localhost:8080"
```

Default inventory admin login (seeded via `application-inventory-local.yml`):

- username: `inventory.admin`
- password: `Admin123!`

## Runtime authorization model (v2 persistent)

- Backend capability contract: `GET /api/v1/admin/capabilities`
- Backend RBAC matrix contract: `GET /api/v1/admin/authorization/matrix`
- Backend RBAC management contracts:
  - `GET /api/v1/admin/authorization/permissions`
  - `GET /api/v1/admin/authorization/roles`
  - `POST/PATCH/DELETE /api/v1/admin/authorization/roles`
  - `PUT /api/v1/admin/authorization/roles/{roleId}/permissions`
  - `GET/PUT /api/v1/admin/authorization/users/{userId}/roles`
  - `GET /api/v1/admin/authorization/audit-logs`
- Router + navigation are capability-aware and no longer hard-coded as admin-only for all warehouse routes.
- Roles & permissions page route is capability-gated (`governance.rbac`) and can be granted by permission-derived capabilities.

Role intent:
- `ADMIN`: full commerce + warehouse + tool access
- `WAREHOUSE_MANAGER`: warehouse operations access
- `VIEWER`: warehouse read-oriented access

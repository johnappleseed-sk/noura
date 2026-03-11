# Noura Admin Dashboard

React admin dashboard for the Noura inventory service.

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

## Runtime authorization model (v1.0 foundation)

- Backend capability contract: `GET /api/v1/admin/capabilities`
- Router + navigation are capability-aware and no longer hard-coded as admin-only for all warehouse routes.

Role intent:
- `ADMIN`: full commerce + warehouse + tool access
- `WAREHOUSE_MANAGER`: warehouse operations access
- `VIEWER`: warehouse read-oriented access

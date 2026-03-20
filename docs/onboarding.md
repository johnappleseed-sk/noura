# Developer Onboarding

## Project Architecture Overview
This repository is a multi-surface commerce platform:
- `archive/legacy-monolith/backend-monolith/`: Spring Boot monolith retained during extraction
- `apps/admin-web/`: React admin application
- `apps/storefront-web/`: customer web storefront
- `apps/api-gateway/`: Spring Cloud Gateway

Primary API prefix: `/api/v1` (commerce) with additional inventory-focused routes where applicable.

## Backend Structure
- Controllers: `backend/src/main/java/com/noura/platform/controller/`
- Services: `backend/src/main/java/com/noura/platform/service/`
- Service implementations: `backend/src/main/java/com/noura/platform/service/impl/`
- Domain entities: `backend/src/main/java/com/noura/platform/domain/entity/`
- DTOs: `backend/src/main/java/com/noura/platform/dto/`
- Repositories: `backend/src/main/java/com/noura/platform/repository/`
- Migrations: `backend/src/main/resources/db/migration/`
These paths are now under `archive/legacy-monolith/backend-monolith/`.

## Frontend Structure
### Admin Dashboard (`apps/admin-web/src`)
- App shell/routing: `app/`
- Feature pages: `pages/` and `features/`
- API clients: `shared/api/endpoints/`
- Auth/capability guards: `shared/auth/`
- Shared UI: `shared/ui/`

### Storefront (`apps/storefront-web`)
- App routes and components follow Next.js app-router conventions.
- Includes predictive header search, scalable catalog pagination/sorting controls, and client-persistent wishlist flow.
- Reference doc: `docs/frontend/storefront-enterprise-hardening.md`

## Database Overview
- Backend uses relational schema with Flyway migrations.
- Core entities include users, products, categories, orders, payments, inventory, and governance/audit records.
- Add schema changes only through new migration files in `db/migration`.
- RBAC persistence is introduced by `V20__admin_rbac_persistent_storage.sql` (`admin_roles`, `admin_permissions`, `admin_role_permissions`, `admin_user_roles`).
- RBAC governance audit logging is introduced by `V21__admin_rbac_audit_logs.sql` (`admin_rbac_audit_logs`).
- RBAC audit integrity controls are introduced by `V22__admin_rbac_audit_integrity_controls.sql` (`payload_hash`, immutable update/delete triggers).
- RBAC bulk saved views are introduced by `V24__admin_rbac_bulk_assignment_saved_views.sql` (`admin_bulk_user_role_views`).

## Run Locally
### Preferred extracted-stack workflow
```bash
cp platform/scripts/.env.example platform/scripts/.env
./platform/scripts/run-local.sh
```

This command:
- starts PostgreSQL, Redis, Kafka, and Keycloak
- bootstraps the shared local PostgreSQL schema for the extracted services
- seeds one demo product so catalog and search are not empty on first boot
- launches the extracted Java services, the gateway, the storefront, and the admin app

Stop everything with:

```bash
./platform/scripts/stop-local.sh
./platform/scripts/stop-local.sh --down-infra
```

### Legacy monolith backend only
```bash
cd archive/legacy-monolith/backend-monolith
./mvnw spring-boot:run
```

## Run Migrations
Migrations run during backend startup when Flyway is enabled.  
For policy checks and environment guards, review `backend/README.md` and workflow `.github/workflows/backend-schema-policy.yml`.
Monolith docs now live at `archive/legacy-monolith/backend-monolith/README.md`.

For the extracted-stack local bootstrap, note:
- service-owned Flyway migrations still share one local PostgreSQL schema
- `platform/scripts/bootstrap-local-db.sh` compensates for that local-only baseline limitation so first-time startup is reproducible

## How To Add New Features
1. Audit existing module patterns first.
2. Add/extend DTOs, service interfaces, service implementations, and controller endpoints.
3. Add or update tests (service and controller/security).
4. Wire frontend API client + page/component state + guarded routes/actions.
5. Update docs:
   - `docs/features/`
   - `docs/api/`
   - `docs/frontend/`
   - `docs/architecture/`
   - `docs/database/` when schema changes
   - `docs/CHANGELOG.md`

## RBAC Governance Module
- Backend services:
  - `AdminAuthorizationService` for matrix/capability resolution
  - `AdminRoleManagementService` for role and assignment mutation workflows
- Controllers:
  - `AdminDashboardController` (`/admin/capabilities`, `/admin/authorization/matrix`)
  - `AdminAuthorizationController` (`/admin/authorization/*` management APIs + preset/bulk preview+assignment APIs + `/admin/authorization/users/roles/bulk/views` + `/admin/authorization/audit-logs` + `/admin/authorization/audit-logs/export`)
- Frontend:
  - `apps/admin-web/src/pages/RolesPermissionsPage.jsx`
  - `apps/admin-web/src/shared/api/endpoints/adminAuthorizationApi.js`
  - Bulk assignment UX includes server-persisted saved views + CSV import/export + row-level validation report + pre-apply diff preview.
- Docs:
  - `docs/features/rbac-authorization-matrix.md`
  - `docs/api/admin-authorization-matrix.md`
  - `docs/frontend/roles-permissions-page.md`
  - `docs/database/rbac-modeling.md`

## Coding Standards
- Keep changes modular and architecture-aligned.
- Avoid touching unrelated files.
- Use explicit loading/error/empty states in UI.
- Enforce authorization in backend and frontend (frontend never replaces backend auth checks).
- Prefer typed/validated DTO boundaries and centralized error mapping.
- Add meaningful file/function comments for new major modules and complex business logic.

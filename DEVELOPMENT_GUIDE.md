# Development Guide

## 1. Local Setup

## Backend

```cmd
cd backend
set JWT_SECRET=replace_with_a_long_random_secret_min_32_chars
mvnw.cmd spring-boot:run
```

Backend default URL: `http://localhost:8080`

## Frontend

```cmd
cd frontend
npm install
npm run dev
```

Frontend default URL: `http://localhost:5173`

## Full stack via Docker

```cmd
set JWT_SECRET=replace_with_a_long_random_secret_min_32_chars
docker compose up --build
```

## 2. Daily Commands

## Backend

```cmd
cd backend
mvnw.cmd test
```

## Frontend

```cmd
cd frontend
npm run typecheck
npm run build
npm test -- --runInBand
```

## 3. Coding Conventions

## Backend Conventions

- Keep controllers thin.
- Put business logic in service implementations.
- Keep entity-to-DTO mapping in mapper classes.
- Enforce auth/ownership in service layer.
- Reuse `PricingService` for coupon and total calculations.
- Keep write operations in transactions.
- Return API data using `ApiResponse`.

## Frontend Conventions

- Keep backend calls in `api/*` modules.
- Keep route/view composition in `pages/*` and `layouts/*`.
- Keep shared app state in Redux slices (`features/*`).
- Use route guards for auth and role-based pages.

## 4. File Organization Rules

- Backend:
  - `controller`: HTTP layer only
  - `service` / `service/impl`: use case logic
  - `repository`: database interfaces
  - `domain`: entities and enums
  - `config`: cross-cutting setup
  - `dto`: contract models
- Frontend (active path):
  - `src/admin/app`: store/providers/router bootstrapping
  - `src/admin/api`: Axios client and endpoint wrappers
  - `src/admin/features`: Redux slices
  - `src/admin/pages`: route pages
  - `src/admin/layouts` and `src/admin/routes`: shell and route guards

## 5. Add a New Backend Endpoint

1. Add request/response DTO(s) in `backend/src/main/java/com/noura/platform/dto`.
2. Add service interface method in `service/`.
3. Implement method in `service/impl/` with:
   - business rules
   - ownership checks
   - role checks (`@PreAuthorize` if needed)
4. Add controller route in `controller/`.
5. Add repository method/query if required.
6. Add tests in `src/test`.
7. Document endpoint in [API_REFERENCE.md](./API_REFERENCE.md).

Controller template:

```java
@PostMapping("/resource")
public ApiResponse<ResourceDto> create(@Valid @RequestBody ResourceRequest request, HttpServletRequest http) {
    return ApiResponse.ok("Resource created", resourceService.create(request), http.getRequestURI());
}
```

## 6. Add a New Frontend Page (Admin Path)

1. Create page file in `frontend/src/admin/pages`.
2. Register route in `frontend/src/admin/app/router.tsx`.
3. Add menu item in `frontend/src/admin/layouts/AdminLayout.tsx` if navigable from sidebar.
4. Add API call in `frontend/src/admin/api/adminApi.ts` (or relevant module).
5. Add/extend Redux slice in `frontend/src/admin/features` if stateful.
6. Add tests if behavior is complex.

## 6.1 Evolve Category Taxonomy Safely

When changing category structures in enterprise catalogs, apply this workflow:

1. Propose taxonomy change:
   - define target hierarchy and business reason
   - define ownership and impacted channels/regions
2. Apply data-safe migration:
   - add new categories first
   - re-map products/variants
   - retire old categories only after validation
3. Validate search/filter behavior:
   - ensure attribute facets still resolve correctly
   - verify category tree and product listing endpoints
4. Review analytics impact:
   - confirm category KPIs remain comparable or provide mapping notes
5. Record change:
   - update architecture/docs and changelog for taxonomy contracts

## 7. Add a Database Migration

1. Create next Flyway file in:
   - `backend/src/main/resources/db/migration/V{next}__description.sql`
2. Keep migration forward-only and deterministic.
3. Do not modify already-applied migration files.
4. Update entity and repository code as needed.
5. Run backend startup or tests to ensure migration applies cleanly.

Example:

```sql
ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS external_reference VARCHAR(128);
```

## 8. Security Development Rules

- Never commit secrets.
- Never log access token/refresh token/password-reset raw token values.
- Keep JWT secret environment-based only.
- Keep CORS origin list explicit in staging/prod.
- Keep ownership checks for user-scoped resources:
  - cart items
  - addresses/payment methods
  - notifications
  - quick reorder source order
- Keep rate-limit proxy trust configuration strict.

## 9. Pull Request Checklist

- Backend tests pass.
- Frontend typecheck/build/tests pass.
- API contract changes are documented.
- Migration scripts exist for schema changes.
- Role checks and ownership checks covered by tests.
- No secrets or sensitive logs introduced.

## 10. Known Constraints to Keep in Mind

- Current frontend default build target is `src/admin` (`@` alias points there).
- `src/enterprise` path exists but is not the default Vite build target.
- Backend Dockerfile currently references `enterprise-commerce-api-1.0.0.jar` while project version is `1.1.0`.
- Rate limiting is in-memory only (not distributed across app instances).

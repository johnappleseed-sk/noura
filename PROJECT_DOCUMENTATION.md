# PROJECT_DOCUMENTATION

## 1. Project Overview

NOURA is an enterprise-oriented commerce platform that combines:
- Product catalog and inventory management
- Cart and checkout flows (including B2B invoice mode)
- Order lifecycle and timeline tracking
- Notification delivery (API + Redis pub/sub + WebSocket fan-out)
- Admin operations for products, stores, users, orders, and approvals

### Main Purpose

The project provides a backend API and frontend dashboard to manage and operate a multi-store commerce experience with B2C and B2B capabilities.

### Key Technologies Used

- Java 21, Spring Boot 3.3.x
- PostgreSQL + Flyway migrations
- Redis (cache, session, pub/sub)
- JWT authentication with Spring Security
- React 19 + Vite + TypeScript
- Redux Toolkit + Axios + Ant Design
- Docker Compose for local orchestration

---

## 2. Tech Stack

- Backend: Spring Boot, Spring Web, Spring Data JPA, Spring Security, Spring Validation
- API Docs: springdoc OpenAPI (`/swagger-ui`, `/v3/api-docs`)
- Database: PostgreSQL 16
- Database Migration: Flyway
- Caching/Session/Realtime fan-out: Redis 7 + Spring Data Redis + Spring Session
- Messaging (optional): Kafka (eventing profile)
- Search integration path (optional): Elasticsearch (search profile)
- Rate limiting: Bucket4j (in-memory key buckets)
- Frontend build/runtime: React 19, TypeScript 5, Vite 7
- Frontend state: Redux Toolkit
- Frontend HTTP: Axios
- Frontend UI: Ant Design + Recharts
- Testing:
  - Backend: JUnit + Spring Boot Test + Spring Security Test
  - Frontend: Jest + Testing Library
- Infrastructure: Docker, Docker Compose

---

## 3. Project Structure

```text
.
├── backend/
│   ├── src/main/java/com/noura/platform/
│   │   ├── config/           # Security, CORS, rate limiting, cache, Redis, OpenAPI, startup validators
│   │   ├── controller/       # REST endpoints under /api/v1/*
│   │   ├── service/          # Business interfaces
│   │   ├── service/impl/     # Business implementations (transactions, ownership checks, events)
│   │   ├── repository/       # Spring Data JPA repositories
│   │   ├── domain/           # JPA entities + enums
│   │   ├── security/         # JWT provider/filter + user details service
│   │   ├── dto/              # Request/response contracts
│   │   ├── mapper/           # Entity -> DTO mappers
│   │   ├── common/           # ApiResponse wrapper, pagination, global exception handling
│   │   ├── listener/         # Domain/Kafka event listeners
│   │   └── search/           # Search gateway abstraction (JPA / Elasticsearch-backed)
│   ├── src/main/resources/
│   │   ├── application*.yml  # Profile-based configuration
│   │   └── db/migration/     # Flyway schema/data migrations
│   ├── src/test/             # Unit, service, and security tests
│   └── Dockerfile
├── frontend/
│   ├── src/main.tsx          # Frontend bootstrap
│   ├── src/admin/            # Active app path (current Vite alias target)
│   ├── src/enterprise/       # Alternate enterprise storefront/admin path
│   ├── src/components|pages  # Legacy storefront path
│   ├── edge/                 # Edge geo-routing middleware example
│   ├── scripts/              # Local helper scripts
│   ├── vite.config.ts        # Alias config (currently @ -> src/admin)
│   └── Dockerfile
├── scripts/
│   └── add-doc-comments.cjs  # Internal code comment generation utility
├── docker-compose.yml
├── CHANGELOG.md
└── GUIDELINES.md
```

Notes:
- Frontend build/typecheck currently target `src/admin` via alias and tsconfig include.
- `src/enterprise` is present and tested separately with Jest alias mapping.

---

## 4. Architecture

Detailed architecture: [ARCHITECTURE.md](./ARCHITECTURE.md)

### Frontend to Backend Communication

1. Frontend uses Axios clients (`src/admin/api/client.ts`, `src/enterprise/api/axiosClient.ts`).
2. JWT access token is read from local storage and sent as `Authorization: Bearer <token>`.
3. Backend validates JWT in `JwtAuthenticationFilter`.
4. Role/permission checks happen in Spring Security + `@PreAuthorize` service methods.

### Data Flow (Catalog/Cart/Checkout)

1. Product/store reads hit controllers -> services -> repositories.
2. Cached paths (`products`, `stores`, `recommendations`) are served via Redis cache manager.
3. Cart totals and coupon checks run through `PricingService` (single source of truth).
4. Checkout creates order + order items, decrements stock atomically, emits events, clears cart.
5. Timeline events and notifications are appended as side effects.

### Authentication Flow

1. `/api/v1/auth/register` creates user and issues access/refresh tokens.
2. `/api/v1/auth/login` authenticates credentials and issues tokens.
3. `/api/v1/auth/refresh` rotates refresh token and issues new token pair.
4. Access token is required for protected endpoints.

### Important Services

- `AuthServiceImpl`: register/login/refresh/password-reset
- `ProductServiceImpl`: catalog CRUD, media, variant, inventory, reviews
- `StoreServiceImpl`: store catalog + nearest-store logic
- `CartServiceImpl`: cart ownership, stock-aware mutations, coupon apply
- `PricingServiceImpl`: subtotal/discount/shipping rules + coupon validity window
- `CheckoutServiceImpl`: idempotent checkout + stock reservation + event publish
- `OrderServiceImpl`: admin order management + status transitions + timeline
- `NotificationServiceImpl`: read-state management + user/broadcast delivery
- `AdminDashboardServiceImpl`: summary and approval workflow

### Enterprise Category Management Model

NOURA now supports the baseline of enterprise category management and defines a clear target model for advanced capabilities:

- Hierarchical category trees:
  - Supports parent-child taxonomy (`Electronics -> Computers -> Laptops`) with tree retrieval endpoint.
- Category governance (target):
  - Category ownership, approval workflow, naming standards, and audit policy should be enforced in service workflows.
- Attribute-based categorization:
  - JSONB product/variant attributes + reusable attribute sets support faceted search and comparison.
- AI-driven auto categorization (target):
  - Planned for NLP/image-assisted classification and taxonomy cleanup suggestions.
- Multi-channel taxonomy mapping (target):
  - Planned mapping layer for marketplace-specific category IDs.
- Category performance analytics (target):
  - Planned KPIs: revenue by category, conversion, turnover, margin, discoverability.
- Localization and regional taxonomy (target):
  - Planned localized names/synonyms for region-specific UX/SEO.
- Enterprise system integration:
  - Existing integration foundations with search/eventing; taxonomy-specific ERP/PIM/CMS contracts are next.

---

## 5. Setup Instructions

### Required Software

- Java 21
- Maven 3.9+ (or use `mvnw.cmd`)
- Node.js 22+ (project Dockerfile uses Node 22)
- npm 10+
- PostgreSQL 16
- Redis 7
- Docker Desktop (recommended for full local stack)

### Environment Variables (Backend)

Required:
- `JWT_SECRET` (must be at least 32 chars, especially in non-local profiles)

Common:
- `SERVER_PORT` (default `8080`)
- `DB_URL` (default `jdbc:postgresql://localhost:5432/noura`)
- `DB_USERNAME` (default `noura`)
- `DB_PASSWORD` (default `noura`)
- `REDIS_HOST` (default `localhost`)
- `REDIS_PORT` (default `6379`)
- `SPRING_PROFILES_ACTIVE` (`local`, `staging`, `prod`)
- `CORS_ALLOWED_ORIGINS` (comma-separated)
- `JWT_ACCESS_MINUTES` (default `30`)
- `JWT_REFRESH_DAYS` (default `14`)
- `KAFKA_ENABLED` (`false` by default)
- `ELASTIC_ENABLED` (`false` by default)

Rate-limit controls:
- `RATE_LIMIT_CAPACITY`
- `RATE_LIMIT_REFILL_TOKENS`
- `RATE_LIMIT_REFILL_MINUTES`
- `RATE_LIMIT_KEY_TTL_MINUTES`
- `RATE_LIMIT_MAX_KEYS`
- `RATE_LIMIT_TRUST_FORWARDED_HEADERS`
- `RATE_LIMIT_FORWARDED_IP_HEADER`
- `RATE_LIMIT_TRUSTED_PROXY_ADDRESSES`

### Environment Variables (Frontend)

- `VITE_API_BASE_URL` (default `http://localhost:8080/api/v1`)
- `VITE_APP_NAME` (used by enterprise path)
- `VITE_ENABLE_AI_FEATURES` (enterprise path)
- `VITE_WS_URL` (enterprise path for realtime WebSocket)
- `VITE_ENABLE_REALTIME_NOTIFICATIONS` (enterprise path)

### Run with Docker Compose

```cmd
set JWT_SECRET=replace_with_a_long_random_secret
docker compose up --build
```

Optional profiles:

```cmd
docker compose --profile eventing --profile search up --build
```

### Run Locally (Without Docker)

Backend:

```cmd
cd backend
set JWT_SECRET=replace_with_a_long_random_secret
mvnw.cmd spring-boot:run
```

Frontend:

```cmd
cd frontend
npm install
npm run dev
```

### Development Verification Commands

Backend:

```cmd
cd backend
mvnw.cmd test
```

Frontend:

```cmd
cd frontend
npm run typecheck
npm run build
npm test -- --runInBand
```

---

## 6. API Documentation

Complete API reference with endpoint-by-endpoint examples: [API_REFERENCE.md](./API_REFERENCE.md)

### API Base

- Base URL: `/api/v1`
- Envelope:

```json
{
  "success": true,
  "message": "Operation message",
  "data": {},
  "timestamp": "2026-03-05T10:00:00Z",
  "path": "/api/v1/example"
}
```

Error envelope:

```json
{
  "success": false,
  "message": "Error message",
  "error": {
    "code": "ERROR_CODE",
    "detail": "Detailed explanation"
  },
  "timestamp": "2026-03-05T10:00:00Z",
  "path": "/api/v1/example"
}
```

### Endpoint Groups

- Auth: register/login/refresh/password reset
- Products: list/details/CRUD/reviews/media/variants/inventory/related
- Stores: list/nearest/preferred/CRUD
- Cart: view/add/update/remove/clear/apply coupon
- Checkout: staged previews + direct confirm
- Orders: admin list/detail/timeline/status updates
- Notifications: my feed, unread counts, read-state changes, admin push/broadcast
- Search and Recommendations: predictive/trend tags/AI and curated lists
- Account: profile, addresses, payment methods, order history, quick reorder, company profile
- Admin: dashboard summary, approval queue updates, user management

### Minimal Request/Response Example

Request:

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "StrongPass123"
}
```

Response:

```json
{
  "success": true,
  "message": "Logged in",
  "data": {
    "userId": "8e3f1f95-8b8c-4b31-a8dc-1f5f66b2af10",
    "email": "admin@example.com",
    "fullName": "Admin User",
    "roles": ["ADMIN"],
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "5a8f2f38-..."
  },
  "timestamp": "2026-03-05T10:00:00Z",
  "path": "/api/v1/auth/login"
}
```

---

## 7. Development Guidelines

Detailed contributor workflow: [DEVELOPMENT_GUIDE.md](./DEVELOPMENT_GUIDE.md)

### Coding Conventions

- Keep controllers thin; put business logic in services.
- Keep authorization and ownership checks explicit in service methods.
- Reuse `PricingService` for all coupon and total calculations.
- Use DTOs for API boundaries; map entities via mapper classes.
- Keep write flows transactional.
- For frontend, keep API access centralized in API client modules.

### File Organization

- Backend:
  - `controller`: transport/API layer
  - `service`: use-case/business layer
  - `repository`: persistence contracts
  - `domain`: entities and enums
  - `config`: cross-cutting infrastructure/security
- Frontend:
  - `src/admin`: current active dashboard app
  - `src/enterprise`: alternate enterprise app path
  - `api`, `features`, `pages`, `components`: feature-oriented organization

### How to Add New Features

- Backend:
  1. Add DTO(s)
  2. Add service interface method
  3. Implement service logic + authorization
  4. Add controller endpoint
  5. Add tests
  6. Add migration if schema changes
- Frontend:
  1. Add/extend API client
  2. Add slice/thunk or component state handling
  3. Add page/component
  4. Wire route
  5. Add tests

---

## 8. Security Guidelines

### How Authentication Works

- Stateless JWT bearer auth.
- Access token generated by `JwtTokenProvider`.
- JWT claims include:
  - `sub` (email)
  - `uid` (user ID)
  - `roles` (role list)
- Refresh token stored server-side in DB with revocation flag.

### Token Handling

- Frontends store tokens in local storage.
- Axios interceptors attach `Authorization` header.
- On `401`, clients clear local token state.

### Secret Management

- `JWT_SECRET` must come from environment variables.
- In non-local profiles, startup fails for missing/weak/default-like secrets.
- Do not commit secrets or tokens to repo/logs.

### Things Developers Must NOT Do

- Do not hardcode JWT secrets in code or config files.
- Do not use wildcard CORS origins in staging/prod.
- Do not log access tokens, refresh tokens, or password-reset tokens.
- Do not bypass ownership checks for user-scoped data.
- Do not implement coupon logic outside `PricingService`.

### Additional Controls in Place

- Security headers (HSTS, CSP, frame deny, no-referrer, content-type options)
- Request correlation ID propagation via `X-Correlation-ID`
- Startup validators for JWT secret and CORS config
- Rate limiting with proxy trust controls
- Password reset tokens stored as SHA-256 hash (`token_hash`)

---

## 9. Common Tasks

### Add a New API Endpoint

1. Create request/response DTO in `backend/src/main/java/com/noura/platform/dto/...`.
2. Add service interface method and implementation.
3. Apply authorization (`@PreAuthorize`) and ownership checks.
4. Expose controller route under `/api/v1`.
5. Add/adjust repository queries if needed.
6. Add tests in `src/test`.
7. Update [API_REFERENCE.md](./API_REFERENCE.md).

Example controller pattern:

```java
@PostMapping("/example")
public ApiResponse<ExampleDto> create(@Valid @RequestBody ExampleRequest request, HttpServletRequest http) {
    return ApiResponse.ok("Example created", exampleService.create(request), http.getRequestURI());
}
```

### Add a New Frontend Page

1. Add page component in active app path (`src/admin/pages` currently).
2. Add route in `src/admin/app/router.tsx`.
3. If role-gated, wrap with `RoleRoute`.
4. Add API call in `src/admin/api`.
5. Add menu entry in `AdminLayout` if needed.
6. Add tests.

### Add Database Migrations

1. Create a new Flyway file:
   - `backend/src/main/resources/db/migration/V{next}__description.sql`
2. Keep migration idempotent where possible.
3. Avoid editing old migration files already applied.
4. Start backend and confirm Flyway applies migration.
5. Add/adjust entity and repository mappings.

Example:

```sql
ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS external_reference VARCHAR(128);
```

---

## 10. Future Improvements

### Scalability

- Move rate limiting from in-memory map to Redis-backed distributed buckets.
- Add proper search indexing/querying for Elasticsearch gateway (current implementation falls back to JPA-style name matching).
- Add asynchronous outbox/event relay for reliable Kafka publishing.

### Maintainability

- Unify frontend app paths (`src/admin`, `src/enterprise`, legacy `src/*`) behind one clear build target.
- Align frontend aliasing/build config and remove dead/duplicate API clients.
- Resolve backend Dockerfile jar-version mismatch (`1.0.0` jar name vs `1.1.0` project version).
- Add architecture decision records (ADRs) for security and module split.

### Performance

- Improve dashboard summary query strategy (avoid full-table scans in memory).
- Add pagination for large admin list reads beyond current defaults.
- Introduce frontend route-based code splitting for large bundle warnings.

### Security and Reliability

- Implement refresh-token rotation enforcement on frontend clients.
- Add CSRF-safe strategy if cookies are introduced in future.
- Expand audit logging around admin write operations.
- Add end-to-end tests for checkout idempotency and notification delivery.

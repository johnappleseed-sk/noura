# Platform Infra Bootstrap (Local)

This folder provides the Phase 1 local platform baseline for microservice evolution:

- `apps/api-gateway` (Spring Cloud Gateway)
- `archive/legacy-monolith/backend-monolith` (current modular backend during transition)
- `services/catalog-service` (catalog extraction slice)
- `services/search-service` (search discovery extraction slice)
- `services/notification-service` (first controlled extraction slice)
- PostgreSQL
- Redis
- Kafka (KRaft single-node)
- Keycloak

## Environment Variable Strategy

1. Copy `.env.example` to `.env`.
2. Keep infra-wide values in this single file (ports, DB users/passwords, JWT secret, auth toggles).
3. Runtime values are injected through Docker Compose environment variables.
4. Service-specific config remains in each service's `application*.yml`.

This keeps local startup simple while staying compatible with later per-environment secrets management.

## Local Startup

```bash
cd platform/scripts
cp .env.example .env
docker compose -f docker-compose.local.yml up -d --build
```

## Health Endpoints

- Gateway actuator: `http://localhost:8080/actuator/health`
- Gateway probe: `http://localhost:8080/internal/health`
- App service (through gateway): `http://localhost:8080/internal/app/health`
- App service readiness (through gateway): `http://localhost:8080/internal/app/readiness`
- Notification service (through gateway): `http://localhost:8080/internal/notification-service/health`
- Notification service readiness (through gateway): `http://localhost:8080/internal/notification-service/readiness`
- Catalog service (through gateway): `http://localhost:8080/internal/catalog-service/health`
- Catalog service readiness (through gateway): `http://localhost:8080/internal/catalog-service/readiness`
- Search service (through gateway): `http://localhost:8080/internal/search-service/health`
- Search service readiness (through gateway): `http://localhost:8080/internal/search-service/readiness`
- Keycloak: `http://localhost:18080/health/ready`

## Route Baseline

- `/api/v1/search/**` -> `search-service`
- `/api/search/**` -> `search-service` (legacy rewrite to `/api/v1/search/**`)
- `/swagger-ui/**` and `/v3/api-docs/**` -> `app-service`
- `/internal/notifications/**` -> `notification-service`
- `/api/v1/categories/tree` + `/api/v1/products/**` -> `catalog-service`
- `/api/**` -> `app-service`
- `/keycloak/**` -> `keycloak` (prefix stripped)

See `k8s-base/config/gateway/routes.example.yml` for a split-service route template (`catalog-governance`, `inventory-availability`, `search-discovery`, `commerce-core`).

## Auth Forwarding Baseline

- Gateway forwards incoming `Authorization` header downstream.
- Optional JWT validation at gateway is controlled by `GATEWAY_AUTH_ENABLED`.
- When auth is enabled, gateway also forwards derived identity headers:
  - `X-Auth-Subject`
  - `X-Auth-Username`
  - `X-Auth-Roles`

Use this as the initial compatibility layer while domains are gradually extracted.

## Controlled Extraction Toggle

- Default local-platform mode routes monolith notification pushes to the extracted service: `APP_NOTIFICATIONS_REMOTE_ENABLED=true`.
- Keep `APP_NOTIFICATIONS_REMOTE_BASE_URL=http://notification-service:8080`.
- Optional hardening: set `NOTIFICATION_INTERNAL_API_KEY` and `APP_NOTIFICATIONS_REMOTE_INTERNAL_API_KEY` to the same value.
- If you need legacy behavior temporarily, set `APP_NOTIFICATIONS_REMOTE_ENABLED=false`.
- With fallback enabled (`APP_NOTIFICATIONS_REMOTE_FALLBACK_LOCAL=true`), monolith automatically falls back to local notification dispatch if remote is unavailable.

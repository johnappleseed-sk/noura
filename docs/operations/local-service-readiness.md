# Local Service Readiness

This document captures the operational baseline expected from extracted NOURA services when they run in local or dev environments.

## Shared Runtime Conventions

- Host Java baseline: `JDK 25` for `apps/api-gateway` and all extracted Spring services when they run outside Docker.
- Default HTTP port: `8080` unless `SERVER_PORT` overrides it.
- Health endpoint: `GET /actuator/health`
- Readiness endpoint: `GET /actuator/health/readiness`
- Liveness endpoint: `GET /actuator/health/liveness`
- Correlation header: `X-Correlation-ID`
- Structured access log format:

```text
http_request correlationId=<id> method=<verb> path=<path> status=<code> durationMs=<ms>
```

- Startup summary log format:

```text
service_startup service=<name> port=<port> health=/actuator/health readiness=/actuator/health/readiness liveness=/actuator/health/liveness profiles=<profiles>
```

- Docker runtime healthcheck:
  - each service image probes `http://localhost:8080/actuator/health/liveness`

## Local Startup Order

Recommended order for a clean local bring-up:

1. PostgreSQL and shared infrastructure from `platform/scripts/docker-compose.local.yml`
2. `catalog-service`, `inventory-service`, `pricing-service`, `customer-service`, `promotion-service`
3. `cart-service`, `order-service`, `payment-service`, `shipping-service`, `notification-service`, `review-service`, `search-service`
4. `checkout-service`
5. `api-gateway`
6. `apps/storefront-web`
7. `apps/admin-web`

This order minimizes downstream startup noise because the orchestration services depend on the foundational data services already being reachable.

## Dependency Map

- `catalog-service`
  - PostgreSQL shared catalog schema
- `search-service`
  - PostgreSQL
  - read-only catalog source data for rebuilds
- `pricing-service`
  - PostgreSQL
- `cart-service`
  - PostgreSQL
  - `catalog-service`
  - `inventory-service`
  - `pricing-service`
  - `promotion-service`
- `checkout-service`
  - PostgreSQL
  - `cart-service`
  - `customer-service`
  - `pricing-service`
  - `inventory-service`
  - `order-service`
  - `payment-service`
  - `notification-service`
- `order-service`
  - PostgreSQL
  - `cart-service` for quick reorder/cart handoff paths
- `payment-service`
  - PostgreSQL
  - `order-service`
- `inventory-service`
  - PostgreSQL
- `customer-service`
  - PostgreSQL
- `promotion-service`
  - PostgreSQL
- `shipping-service`
  - PostgreSQL
  - `order-service`
- `notification-service`
  - PostgreSQL
- `review-service`
  - PostgreSQL
  - `catalog-service`
- `api-gateway`
  - extracted backend services
  - Keycloak when `GATEWAY_AUTH_ENABLED=true`

## Local Debugging Notes

- Preserve the `X-Correlation-ID` header when reproducing an issue so request logs can be stitched together across gateway and downstream services.
- When running many Spring services directly against the shared local PostgreSQL container, cap Hikari pool size to avoid exhausting default DB slots:
  - `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=3`
  - `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=0`
  - optional: `SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT=10000`
- Probe endpoints are intended to distinguish process health from dependency readiness:
  - `liveness` answers whether the process should stay running
  - `readiness` answers whether the service is ready to serve traffic
- Gateway exposes upstream readiness and liveness passthrough routes under `/internal/<service>/readiness` and `/internal/<service>/liveness`.
- `api-gateway` defaults forwarded and `X-Forwarded-*` header filters to `false` in local/dev runs. Re-enable them explicitly with:
  - `GATEWAY_FORWARDED_HEADERS_ENABLED=true`
  - `GATEWAY_X_FORWARDED_ENABLED=true`
  when the gateway is deployed behind a real proxy/load balancer.

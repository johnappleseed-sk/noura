# api-gateway

Spring Cloud Gateway entry point for NOURA storefront, admin, and internal service probe routing.

## Purpose

- route external HTTP traffic to extracted backend services
- forward auth claims when gateway auth is enabled
- expose internal readiness and liveness passthrough routes for local/dev operations
- emit structured request logs with `X-Correlation-ID`
- use explicit upstream service URLs instead of runtime service discovery in local/dev

## Dependencies

- `catalog-service`
- `search-service`
- `inventory-service`
- `pricing-service`
- `promotion-service`
- `review-service`
- `cart-service`
- `customer-service`
- `order-service`
- `checkout-service`
- `payment-service`
- `shipping-service`
- `notification-service`
- Keycloak when `GATEWAY_AUTH_ENABLED=true`

## Environment variables

- `SERVER_PORT` default `8080`
- `CATALOG_SERVICE_URL` default `http://localhost:8084`
- `SEARCH_SERVICE_URL` default `http://localhost:8085`
- `INVENTORY_SERVICE_URL` default `http://localhost:8086`
- `PRICING_SERVICE_URL` default `http://localhost:8087`
- `CART_SERVICE_URL` default `http://localhost:8088`
- `CUSTOMER_SERVICE_URL` default `http://localhost:8089`
- `ORDER_SERVICE_URL` default `http://localhost:8090`
- `CHECKOUT_SERVICE_URL` default `http://localhost:8091`
- `PAYMENT_SERVICE_URL` default `http://localhost:8092`
- `SHIPPING_SERVICE_URL` default `http://localhost:8093`
- `PROMOTION_SERVICE_URL` default `http://localhost:8094`
- `REVIEW_SERVICE_URL` default `http://localhost:8095`
- `NOTIFICATION_SERVICE_URL` default `http://localhost:8083`
- `APP_SERVICE_URL` default `http://localhost:8082`
- `KEYCLOAK_URL` default `http://localhost:18080`
- `KEYCLOAK_ISSUER_URI` default `http://localhost:18080/realms/noura`
- `GATEWAY_AUTH_ENABLED` default `false`
- `GATEWAY_FORWARD_CLAIMS` default `true`
- `GATEWAY_SUBJECT_HEADER` default `X-Auth-Subject`
- `GATEWAY_USERNAME_HEADER` default `X-Auth-Username`
- `GATEWAY_ROLES_HEADER` default `X-Auth-Roles`

## Run locally

```bash
cd apps/api-gateway
mvn spring-boot:run
```

## Operational basics

- Default port: `8080`
- Health probes:
  - `/actuator/health`
  - `/actuator/health/readiness`
  - `/actuator/health/liveness`
- Internal upstream probe passthrough:
  - `/internal/<service>/health`
  - `/internal/<service>/readiness`
  - `/internal/<service>/liveness`
- Correlation header: `X-Correlation-ID`
- Discovery is intentionally disabled in the gateway. `spring.cloud.discovery.enabled=false`
  keeps actuator health aligned with the explicit-URI routing model and avoids misleading
  `Discovery Client not initialized` health contributors when no registry is deployed.

## Known limitations

- Probe passthrough assumes downstream services are running directly on the configured base URLs.
- Gateway auth remains optional for local/dev startup and does not yet enforce a stricter local profile split.

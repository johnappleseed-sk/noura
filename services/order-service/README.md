# order-service

Production order management service for order creation, customer order history, and admin order operations.

## Exposed endpoints

- `POST /api/v1/orders` (legacy alias: `/api/orders`)
- `GET /api/v1/orders/{orderId}` (legacy alias: `/api/orders/{orderId}`)
- `GET /api/v1/orders/{orderId}/timeline` (legacy alias: `/api/orders/{orderId}/timeline`)
- `GET /api/v1/orders` (legacy alias: `/api/orders`) for admin/internal listing
- `PATCH /api/v1/orders/{orderId}/status` (legacy alias: `/api/orders/{orderId}/status`)
- `GET /api/v1/account/orders` (legacy alias: `/api/account/orders`) for current customer history
- `POST /internal/orders/{orderId}/status` for trusted internal lifecycle transitions

## Identity and authorization behavior

- Preferred customer identity source: `X-Auth-Subject` forwarded by gateway auth.
- Fallback for local/dev when gateway forwarding is absent: bearer token fingerprint from `Authorization`.
- Admin endpoints require one of:
  - `X-Auth-Roles` containing admin-like role (`ADMIN`, `ROLE_ADMIN`, `ORDER_MANAGER`, `ROLE_ORDER_MANAGER`, `SUPER_ADMIN`)
  - valid internal API key when `APP_INTERNAL_API_KEY` is configured.
- Internal lifecycle transitions are intended for trusted service-to-service callers such as `checkout-service` and do not rely on forwarded admin role headers.

## Order lifecycle (v1)

- `PAYMENT_PENDING -> PAID -> PROCESSING -> PACKED -> SHIPPED -> DELIVERED`
- Cancellation/refund side paths are supported:
  - `PAYMENT_PENDING|PAID|PROCESSING|PACKED -> CANCELLED`
  - `PAID|PROCESSING|PACKED|SHIPPED|DELIVERED|CANCELLED -> REFUNDED`

## Determinism and auditability

- Create requests can include `idempotencyKey` for deterministic retry handling per customer.
- Immutable snapshots are copied into orders and order items:
  - checkout/store/customer references
  - monetary totals
  - shipping/billing snapshots
  - line-item snapshots
- Status transition history is persisted in `order_status_history`.

## Internal checkout finalization

- `checkout-service` currently creates orders in `PAYMENT_PENDING`.
- After synchronous payment confirmation succeeds, checkout finalizes the order through `POST /internal/orders/{orderId}/status`.
- This internal path keeps public admin authorization separate from trusted service orchestration.

## Local run

```bash
cd services/order-service
mvn spring-boot:run
```

Required environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Optional environment variables:

- `SERVER_PORT` (default `8080`)
- `APP_INTERNAL_API_KEY`

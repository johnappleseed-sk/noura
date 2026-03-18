# cart-service

Production cart service for persistent customer and guest carts.

## Exposed endpoints

- `GET /api/v1/cart` (legacy alias: `/api/cart`)
- `POST /api/v1/cart/items` (legacy alias: `/api/cart/items`)
- `PUT /api/v1/cart/items/{itemId}` (legacy alias: `/api/cart/items/{itemId}`)
- `DELETE /api/v1/cart/items/{itemId}` (legacy alias: `/api/cart/items/{itemId}`)
- `DELETE /api/v1/cart` and `DELETE /api/v1/cart/items` (legacy aliases under `/api/cart`)
- `POST /api/v1/cart/merge` (legacy alias: `/api/cart/merge`)
- `POST /api/v1/cart/refresh` (legacy alias: `/api/cart/refresh`)
- `POST /api/v1/cart/coupon` (legacy alias: `/api/cart/coupon`)
- `DELETE /api/v1/cart/coupon` (legacy alias: `/api/cart/coupon`)

## Identity and cart ownership

- Customer cart resolution priority:
  - `X-Auth-Subject` header (preferred, forwarded by gateway auth flow)
  - `Authorization: Bearer ...` fingerprint fallback for local/dev compatibility
- Guest cart resolution:
  - `X-Cart-Token` header
  - if absent, service issues a new token and returns it in response payload/header

## Downstream integrations

This service validates each cart line through synchronous REST:

- Catalog: product existence + product snapshot
- Pricing: effective unit price resolution
- Inventory: available quantity checks
- Promotion: coupon validation + discount evaluation

No Kafka/event bus is required for this first cart extraction slice.

## Local run

```bash
cd services/cart-service
mvn spring-boot:run
```

Required environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Optional environment variables:

- `SERVER_PORT` (default `8080`)
- `CATALOG_SERVICE_BASE_URL` (default `http://localhost:8084`)
- `INVENTORY_SERVICE_BASE_URL` (default `http://localhost:8086`)
- `PRICING_SERVICE_BASE_URL` (default `http://localhost:8087`)
- `PROMOTION_SERVICE_BASE_URL` (default `http://localhost:8094`)

## Notes

- Coupon application uses promotion-service deterministic validation (`/api/v1/promotions/validate-code`) and updates `discountAmount`/`totalAmount` in cart totals.
- Shipping amount remains a placeholder in this slice and will be supplied by shipping orchestration later.
- Cart item validation snapshot is persisted for refresh/debug visibility.

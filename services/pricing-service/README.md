# pricing-service

Production pricing service for product price resolution used by storefront and checkout.

## Exposed endpoints

- `GET /api/pricing/v1/prices/products/{productId}`
- `GET /api/pricing/v1/prices/bulk`
- `GET /api/pricing/v1/prices/snapshots/active`
- `POST /api/pricing/v1/admin/prices`
- `PUT /api/pricing/v1/admin/prices`
- `GET /api/v1/price-lists`
- `POST /api/v1/price-lists`
- `POST /api/v1/prices`
- `GET /api/v1/prices/variants/{variantId}`

## Pricing resolution rules (v1)

- Resolve only records where `active=true` and current time is inside `[startsAt, endsAt]` (null bounds are open).
- Scope precedence:
  1. exact `storeId`
  2. exact `channelCode`
  3. global (no store/channel)
- Tie-breakers: highest `priority`, latest `startsAt`, latest `updatedAt`.
- `effectivePrice` currently equals `basePrice` (promotion integration comes later).
- `compareAtPrice` is returned only when greater than `effectivePrice`.

## Local run

```bash
cd services/pricing-service
mvn spring-boot:run
```

Required environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Optional:

- `SERVER_PORT` (default `8080`)

## Notes

- Uses synchronous REST and PostgreSQL only for this extraction slice.
- Currency is validated against `pricing_currencies` reference table.
- No Kafka/event bus dependency is introduced in this version.
- Flyway now owns the lightweight `legacy_price_lists` table used by admin-web pricing compatibility flows.
- Persistence cleanup also enforces a single default currency row through a partial unique index on `pricing_currencies`.

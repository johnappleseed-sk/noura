# inventory-service

Production inventory extraction slice for stock visibility and stock mutation.

## Exposed endpoints

- `GET /api/inventory/v1/stock-levels`
- `GET /api/inventory/v1/stock-levels/products/{productId}`
- `GET /api/inventory/v1/stock-levels/products/{productId}/locations/{locationId}`
- `GET /api/inventory/v1/stock-levels/low-stock`
- `POST /api/inventory/v1/stock-levels/adjustments`
- `POST /api/inventory/v1/stock-levels/reservations`
- `POST /api/inventory/v1/stock-levels/reservations/release`
- `POST /api/inventory/v1/stock-levels/deductions`

Compatibility endpoint for existing admin stock adjustment flow:
- `POST /api/inventory/v1/movements/adjustments`

## Local run

```bash
cd services/inventory-service
mvn spring-boot:run
```

Required environment variables:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Optional:
- `SERVER_PORT` (default `8080`)

## Notes

- Uses synchronous REST and PostgreSQL only for this first extraction slice.
- Keeps inventory movement logging without introducing Kafka/event bus.
- Designed so catalog/search can query stock availability by product ID.
- Flyway migration `V2__inventory_lookup_indexes.sql` adds product and warehouse movement indexes for stock-history and low-latency checkout reservation lookups.

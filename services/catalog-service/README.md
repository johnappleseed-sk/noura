# catalog-service

Real extraction slice for catalog read APIs, reusing the archived monolith schema and contracts.

## Exposed endpoints

- `GET /api/v1/products`
- `GET /api/v1/products/{productId}`
- `GET /api/v1/products/{productId}/inventory`
- `GET /api/v1/products/search?q=...`
- `GET /api/v1/products/trend-tags`
- `GET /api/v1/categories/tree`

## Local run

```bash
cd services/catalog-service
mvn spring-boot:run
```

Required environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

# catalog-service

Real extraction slice for catalog read APIs plus transitional admin recommendation/merchandising compatibility contracts, reusing archived monolith schema and API shapes where practical.

## Exposed endpoints

- `GET /api/v1/products`
- `GET /api/v1/merchandising/products`
- `GET /api/v1/products/{productId}`
- `GET /api/v1/products/{productId}/inventory`
- `GET /api/v1/products/search?q=...`
- `GET /api/v1/products/trend-tags`
- `GET /api/v1/categories/tree`
- `GET /api/v1/recommendations/{best-sellers|trending|deals|personalized|cross-sell|mock-ai}`
- `GET /api/v1/products/{productId}/related`
- `GET /api/v1/products/{productId}/frequently-bought-together`
- `GET /api/v1/admin/recommendations/settings`
- `PUT /api/v1/admin/recommendations/settings`
- `GET /api/v1/admin/recommendations/preview`
- `GET /api/v1/admin/merchandising/settings`
- `PUT /api/v1/admin/merchandising/settings`
- `GET /api/v1/admin/merchandising/boosts`
- `POST /api/v1/admin/merchandising/boosts`
- `PUT /api/v1/admin/merchandising/boosts/{boostId}`
- `DELETE /api/v1/admin/merchandising/boosts/{boostId}`
- `GET /api/v1/admin/merchandising/preview`

## Compatibility notes

- Storefront recommendation rails and product-detail related/FBT rails stay catalog-owned until a dedicated recommendation owner is extracted.
- Admin recommendation and merchandising pages now have backend-compatible contracts without reviving the legacy catch-all `app-service`.
- Recommendation and merchandising control state is intentionally lightweight and deterministic in this slice; it is a compatibility layer, not yet a dedicated long-term control-plane service.

## Local run

```bash
cd services/catalog-service
mvn spring-boot:run
```

Required environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

## Persistence note

- `catalog-service` is currently read-only over the shared catalog schema and intentionally does not own Flyway migrations yet.
- Its JPA mappings reuse canonical tables such as `products`, `product_variants`, `product_media`, `categories`, `brands`, and `stores`.
- Schema evolution for those tables still belongs to the source catalog domain until catalog write ownership is extracted fully.

# search-service

Standalone search-discovery boundary for NOURA.

## Decision Summary

`catalog-service` already contains catalog browse and admin-product search behavior, so this service is intentionally narrower:

- `catalog-service` remains the source of truth for product identity, listing availability, and catalog/admin browse search.
- `search-service` owns the canonical `/api/v1/search/**` discovery contract.
- `search-service` also owns the internal indexing abstraction so PostgreSQL can be replaced with OpenSearch later without changing callers.

This keeps discovery concerns isolated without duplicating catalog ownership.

## What This Service Owns

- product search over search-owned projection documents
- predictive suggestions
- trend tags
- internal product-index upsert, rebuild, and delete operations
- provider abstraction for future external search backends

## Public Endpoints

- `GET /api/v1/search/products?q=...&categoryId=...&brandId=...&page=0&size=20`
- `GET /api/v1/search/predictive?q=...&scope=all|products|brands|categories|stores`
- `GET /api/v1/search/trend-tags`

## Internal Endpoints

- `POST /internal/search/index/products`
- `POST /internal/search/index/products/rebuild`
- `DELETE /internal/search/index/products/{productId}`

Internal indexing routes require `X-Internal-Api-Key` and are designed for future event-driven indexing from catalog, review, or merchandising flows.

## Storage And Provider Model

- Runtime reads use `search_product_documents`, not canonical catalog tables directly.
- The first provider is PostgreSQL-backed and uses trigram/full-text indexes plus a generated `search_document` column.
- Projection rebuilds currently pull from canonical catalog source tables exposed through read-only JPA mappings.
- The provider contract is adapted from archived search adapter patterns so the backing store can move to OpenSearch later.

## Query Ownership Rules

- Blank product search queries return an empty page. `search-service` is not a browse endpoint.
- Store suggestions remain a read-through compatibility path until store discovery is projected separately.
- Catalog browse routes such as `/api/v1/products` and `/api/v1/products/search` stay in `catalog-service`.

## Local Run

```bash
cd services/search-service
mvn spring-boot:run
```

Required environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `API_VERSION_PREFIX` (optional, defaults to `/api/v1`)
- `SEARCH_PROVIDER` (optional, defaults to `postgres`)
- `APP_INTERNAL_API_KEY` (required to enable internal indexing endpoints)

## Notes

- Search remains a projection service, not a system of record.
- Catalog owns product truth; search owns projection and discovery queries.
- Event-driven indexing, OpenSearch adapters, and broader merchandising signals are follow-up work.

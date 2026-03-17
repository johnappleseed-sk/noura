# Search Service API

## Decision Summary

The repository audit found two existing realities:

- `catalog-service` already owns product truth plus browse/admin search behavior.
- storefront and gateway already expose a search-specific contract under `/api/v1/search/**`.

The first extracted `search-service` therefore stays narrow:

- keep `catalog-service` as the owner of `/api/v1/products`, `/api/v1/products/search`, and admin product-search flows
- make `search-service` the canonical owner of `/api/v1/search/**`
- add an internal indexing contract so runtime reads come from a search-owned projection table

## Public Endpoints

Base prefix: `/api/v1/search`

### GET `/products`

Queries search-owned product documents.

Query parameters:

- `q` optional free-text query
- `keyword` optional alias for `q`
- `query` optional compatibility alias matching existing storefront product-listing flows
- `categoryId` optional category filter
- `brandId` optional brand filter
- `page` optional zero-based page index, default `0`
- `size` optional page size, default `20`, max `100`

Behavior:

- blank queries return an empty page so the service does not become a second catalog browse endpoint
- results are limited to active indexed products
- ranking prefers trending, higher popularity, and fresher indexed documents

Example:

```http
GET /api/v1/search/products?q=mug&page=0&size=20
```

Example response body:

```json
{
  "success": true,
  "message": "Product search results",
  "data": {
    "content": [
      {
        "productId": "0a5f0be0-f0e2-46b8-92d5-e86c277df1b0",
        "id": "0a5f0be0-f0e2-46b8-92d5-e86c277df1b0",
        "productCode": "SKU-1001",
        "name": "Travel Mug",
        "slug": "travel-mug",
        "categoryId": "6e2f9cb5-6e72-4e96-95b0-3a5b5053297a",
        "categoryName": "Drinkware",
        "brandId": "4f8ec9ce-a6d8-4a2e-a353-bf0debd6cb89",
        "brandName": "NouraHome",
        "averageRating": 4.8,
        "reviewCount": 18,
        "trending": true,
        "isTrending": true,
        "popularityScore": 88,
        "merchandisingScore": 88
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

### GET `/predictive`

Returns predictive suggestions from indexed documents.

Query parameters:

- `q` required query text
- `scope` optional, one of `all`, `products`, `brands`, `categories`, `stores`

Behavior:

- product, brand, and category suggestions are served from indexed documents
- store suggestions remain a temporary read-through compatibility path from catalog-backed store rows

Example:

```http
GET /api/v1/search/predictive?q=lap&scope=all
```

### GET `/trend-tags`

Returns discovery tags derived from trending indexed documents.

Behavior:

- tags are aggregated primarily from indexed category names
- when the projection is empty, the service returns stable fallback tags for startup-safe discovery screens
- responses expose `value` plus compatibility aliases `name` and `tag` for existing storefront consumers

## Internal Indexing Endpoints

Base prefix: `/internal/search/index`

These routes are intended for trusted internal callers only and require `X-Internal-Api-Key`.

### POST `/products`

Upserts one or more product search documents.

Request example:

```json
{
  "products": [
    {
      "productId": "0a5f0be0-f0e2-46b8-92d5-e86c277df1b0",
      "productCode": "SKU-1001",
      "name": "Travel Mug",
      "slug": "travel-mug",
      "categoryId": "6e2f9cb5-6e72-4e96-95b0-3a5b5053297a",
      "categoryName": "Drinkware",
      "brandId": "4f8ec9ce-a6d8-4a2e-a353-bf0debd6cb89",
      "brandName": "NouraHome",
      "shortDescription": "Vacuum insulated stainless steel mug",
      "active": true,
      "trending": true,
      "popularityScore": 88,
      "averageRating": 4.8,
      "reviewCount": 18,
      "sourceUpdatedAt": "2026-03-18T00:00:00Z"
    }
  ]
}
```

### POST `/products/rebuild`

Rebuilds the product search projection from canonical catalog source tables.

Behavior:

- current implementation deletes and recreates the projection in one transactional rebuild flow
- returns the provider code and indexed document count

### DELETE `/products/{productId}`

Deletes one indexed search document.

## Provider Boundary

`ProductSearchIndexProvider` defines the storage/query abstraction for:

- product search
- predictive suggestions
- trend tags
- document upsert
- rebuild
- delete

The first implementation is PostgreSQL-backed. The interface is intentionally compatible with later OpenSearch or Elasticsearch adapters.

## Ownership Boundaries

- `catalog-service` owns canonical product data and browse/admin product search
- `search-service` owns search projections and search/discovery APIs
- `search-service` does not mutate catalog truth

## Current Limitations

- no OpenSearch adapter yet
- no event bus or outbox-driven indexing yet
- no faceting/aggregations beyond the current filter fields
- no separate projected store-discovery model yet

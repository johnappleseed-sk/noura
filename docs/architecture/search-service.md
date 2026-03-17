# Search Service Architecture

## Decision

Implement `search-service` now, but as a narrow standalone boundary.

Do not move all catalog search behavior out of `catalog-service` yet.

## Audit Outcome

The repository already had two overlapping search implementations:

- `catalog-service` contained:
  - storefront product search and listing queries
  - admin product search
  - predictive suggestions
  - trend tags
- `search-service` existed as a thin predictive/trend facade

The archive also contained reusable search adapter patterns:

- a product-search adapter boundary
- a PostgreSQL-backed implementation
- placeholder Elasticsearch/OpenSearch gateway classes
- PostgreSQL trigram and full-text index migrations

## Boundary Decision

The extracted boundary is:

- `catalog-service`
  - owns canonical product truth
  - owns storefront browse and admin product-search flows
  - remains the source for `/api/v1/products` and `/api/v1/products/search`
- `search-service`
  - owns `/api/v1/search/**`
  - owns search projections
  - owns the indexing abstraction for future search backends

This gives NOURA a real standalone search surface without duplicating catalog ownership.

## First Implementation Shape

### Runtime read model

`search-service` now queries `search_product_documents`, a search-owned projection table.

The projection stores:

- product identity and code
- searchable product name and slug
- category and brand names
- short description text
- active/trending flags
- popularity score
- rating summary fields
- source update timestamp
- index timestamp

### Provider abstraction

`ProductSearchIndexProvider` is the stable search boundary.

The first implementation is `PostgresProductSearchIndexProvider`, which:

- queries projection rows for runtime reads
- rebuilds projection rows from canonical catalog tables
- exposes internal document upsert/delete/rebuild operations

This pattern is adapted from archived search adapter code so OpenSearch can replace PostgreSQL later without breaking callers.

### Query model

Public query APIs are intentionally limited to discovery:

- `GET /api/v1/search/products`
- `GET /api/v1/search/predictive`
- `GET /api/v1/search/trend-tags`

Important rule:

- blank product search keywords return an empty page so `search-service` cannot silently become a browse endpoint

### Indexing model

Internal indexing APIs exist under `/internal/search/index/**`.

Current indexing strategy:

- rebuild from canonical catalog tables
- direct internal upsert/delete for controlled callers
- protect all internal indexing routes with `X-Internal-Api-Key`

Future indexing strategy:

- catalog, review, or merchandising changes publish events
- search-service consumes those events and incrementally updates the projection

## PostgreSQL Search Foundation

The first implementation reuses archive guidance and adds:

- `pg_trgm`
- generated `search_document` tsvector
- trigram indexes on key text columns
- GIN index on the generated full-text document

This keeps the service deterministic and easy to reason about while still using realistic text-search primitives.

## Transitional Duplication

`catalog-service` still contains predictive/trend logic during the migration window.

That duplication is tolerated temporarily because:

- storefront and gateway already route search traffic through `/api/v1/search/**`
- removing catalog-side duplication would widen the task and increase regression risk

The canonical contract should now be treated as:

- `search-service` for search/discovery
- `catalog-service` for catalog browse and product truth

## Follow-Up Work

- add event-driven indexing through outbox or broker consumption
- add OpenSearch adapter behind `ProductSearchIndexProvider`
- project store-discovery data instead of using read-through compatibility queries
- decide whether merchandising boosts and review aggregates should be pushed into the search projection asynchronously

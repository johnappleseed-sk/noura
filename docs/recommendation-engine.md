# Recommendation Engine

## Scope

This module exposes storefront-ready recommendation APIs on top of existing product, order-item, and analytics-event data.

## Endpoints

- `GET /api/v1/recommendations/product/{productId}?limit=6`
- `GET /api/v1/recommendations/trending?limit=8`
- `GET /api/v1/recommendations/best-sellers?limit=8`
- `GET /api/v1/recommendations/deals?limit=8`
- `GET /api/v1/recommendations/personalized?limit=8`
- `GET /api/v1/recommendations/cross-sell?limit=8`
- `GET /api/v1/products/{productId}/related?limit=6`
- `GET /api/v1/products/{productId}/frequently-bought-together?limit=6`

## Security

- Public:
  - product recommendation bundle
  - trending
  - best-sellers
  - deals
  - related products
  - frequently bought together
- Authenticated:
  - personalized recommendations
  - cross-sell recommendations

## Scoring model

- `trending`: popularity score, trending flag, best-seller flag, rating, and analytics weights
- `best-sellers`: best-seller flag, popularity score, rating, and trending boost
- `deals`: flash-sale flag plus detected `salePrice` discount from product attributes
- `personalized`: customer analytics events drive anchor products, then category/brand affinity and co-purchase data rank candidates
- `cross-sell`: commerce-intent events drive anchor products, then order co-purchase data ranks complements

## Current assumptions

- Customer-specific recommendations use `Authentication#getName()` as the recommendation identity key.
- Analytics personalization improves when storefront events include `customerRef`.
- If customer-specific signals are thin, the engine falls back to broader recommendation sets instead of returning empty lists.

## Extension points

- Replace in-memory scoring with materialized recommendation tables or a dedicated retrieval service.
- Incorporate inventory availability, regional catalog visibility, and pricing context into ranking.
- Feed the engine from an async event pipeline instead of synchronous repository scans.

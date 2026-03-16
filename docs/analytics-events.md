# Analytics Events

## Event ingestion

- `POST /api/v1/analytics/events`

### Request (storefront)

The storefront client sends a lightweight event envelope plus a flexible `metadata` map.

```json
{
  "eventType": "PRODUCT_CLICK",
  "sessionId": "uuid-or-random",
  "customerRef": "user@example.com",
  "productId": "c2a8e5a0-7b0f-4d32-8e8e-0f2a89b3fd7a",
  "pagePath": "/",
  "source": "storefront-web",
  "occurredAt": "2026-03-10T10:00:00Z",
  "metadata": {
    "listName": "home-best-sellers-carousel",
    "slot": 2,
    "merchandisingScore": 0.82
  }
}
```

## Merchandising-related event types

- `PRODUCT_IMPRESSION`
- `PRODUCT_CLICK`
- `ADD_TO_CART`
- `CATEGORY_IMPRESSION`
- `CATEGORY_CLICK`

## Rail performance reporting (admin)

- `GET /api/v1/admin/analytics/rails`

### Query parameters

- `from` (optional, default: now - 30 days)
- `to` (optional, default: now)
- `listNamePrefix` (optional, default: `home-`)
- `pagePath` (optional, default: `/`)

### Response

```json
{
  "success": true,
  "message": "Rail performance",
  "path": "/api/v1/admin/analytics/rails",
  "data": {
    "from": "2026-02-09T10:00:00Z",
    "to": "2026-03-10T10:00:00Z",
    "listNamePrefix": "home-",
    "pagePath": "/",
    "rails": [
      {
        "listName": "home-featured-products-grid",
        "impressions": 12000,
        "clicks": 820,
        "addToCart": 210,
        "clickThroughRate": 6.83,
        "clickToCartRate": 25.61,
        "impressionToCartRate": 1.75
      }
    ]
  }
}
```

## Add-to-cart attribution (storefront → backend)

To attribute `ADD_TO_CART` events back to the rail that drove the click, the storefront can include optional list context when adding an item:

- `POST /api/v1/cart/items`
  - `analyticsListName`
  - `analyticsSlot`
  - `analyticsPagePath`

When present, the backend stores `metadata.listName` and `metadata.slot` on the `ADD_TO_CART` analytics record.


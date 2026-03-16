# Merchandising Engine

## Scope

The merchandising engine provides catalog ranking and manual operator controls for storefront browsing and campaign curation.

## Public endpoint

- `GET /api/v1/merchandising/products`

### Query parameters

- `query`
- `categoryId`
- `storeId`
- `sort`: `featured`, `popularity`, `trending`, `bestselling`, `new`, `name`, `priceAsc`, `priceDesc`
- `page`
- `size`

## Admin endpoints

- `GET /api/v1/admin/merchandising/settings`
- `PUT /api/v1/admin/merchandising/settings`
- `GET /api/v1/admin/merchandising/boosts`
- `POST /api/v1/admin/merchandising/boosts`
- `PUT /api/v1/admin/merchandising/boosts/{boostId}`
- `DELETE /api/v1/admin/merchandising/boosts/{boostId}`
- `GET /api/v1/admin/merchandising/preview`

## Ranking inputs

- product popularity score
- aggregated product inventory by store
- scheduled manual boosts
- new-arrival window
- trending flag
- best-seller flag
- low-stock penalty
- product impressions from storefront listing exposure
- product clicks from storefront listing interaction
- click-through rate derived from click / impression signals

## Behavioral signals

- `PRODUCT_IMPRESSION`: emitted when a catalog card becomes visible in the viewport
- `PRODUCT_CLICK`: emitted when a shopper opens a product from the catalog grid
- `ADD_TO_CART`: recorded when a shopper adds a product to the cart (attribution-aware when list context is provided)
- Featured ranking now uses impression volume, click volume, and CTR instead of treating order-item sales velocity as the conversion proxy.
- `bestselling` remains an explicit sales-driven sort backed by historical order items.

## Attribution metadata

Merchandising and rail reporting rely on the following analytics metadata keys:

- `metadata.listName`: stable surface identifier (examples: `home-best-sellers-carousel`, `search-results-grid`)
- `metadata.slot`: 0-based index for the card position inside a grid/rail

## Click-to-cart conversion

To measure rail-level click-to-cart conversion, storefront add-to-cart requests can attach list context:

- `POST /api/v1/cart/items` accepts optional `analyticsListName`, `analyticsSlot`, `analyticsPagePath`
- When present, the backend `ADD_TO_CART` analytics record includes `metadata.listName` and `metadata.slot`

## Notes

- Impression and click contributions are log-scaled so high-volume products do not overwhelm the ranking.
- Manual boosts are schedule-aware and stack when multiple active boosts target the same product.
- The storefront catalog consumes this endpoint directly, so merchandising changes propagate without touching the product service.

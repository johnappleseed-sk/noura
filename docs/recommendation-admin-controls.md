# Recommendation Admin Controls

## Admin endpoints

- `GET /api/v1/admin/recommendations/settings`
- `PUT /api/v1/admin/recommendations/settings`
- `GET /api/v1/admin/recommendations/preview?customerRef=&productId=&limit=`

## Purpose

This admin surface gives merchandising and growth teams direct control over recommendation ranking behavior without code changes.

## Tunable weights

- `productViewWeight`
- `addToCartWeight`
- `checkoutWeight`
- `trendingBoost`
- `bestSellerBoost`
- `ratingWeight`
- `categoryAffinityWeight`
- `brandAffinityWeight`
- `coPurchaseWeight`
- `dealBoost`
- `maxRecommendations`

## Preview behavior

- `customerRef` previews personalized and cross-sell recommendations using the same service path as authenticated storefront traffic.
- `productId` previews related products and frequently bought together bundles for a specific product.
- Preview always uses persisted settings so ranking changes can be reviewed before storefront observation.

## Notes

- If no settings row exists, the system uses service defaults and creates the row only when an admin saves settings.
- Personalized previews degrade gracefully to broader recommendation sets when customer-specific signals are missing.

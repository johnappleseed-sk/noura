# Storefront Enterprise Hardening

## Purpose
Improve the existing Next.js storefront in-place for production readiness without rebuilding architecture:

- stronger search/discovery UX
- scalable catalog browsing controls
- wishlist flow completion
- reduced redundant client API traffic
- better perceived loading performance

## Architecture

### Existing structure retained
- `frontend/storefront-noura/app/*` routes
- `frontend/storefront-noura/components/*` shared UI and feature components
- `frontend/storefront-noura/lib/api.js` backend integration layer

### Added or enhanced modules
- `components/search/HeaderSearch.jsx`
  - debounced predictive search orchestration
  - grouped suggestions by scope
- `lib/wishlist.js`
  - local persistent wishlist storage and cross-tab synchronization
- `components/product/WishlistToggleButton.jsx`
  - reusable wishlist action control for product cards/details
- `lib/cartEvents.js`
  - cart mutation event bus used by header badge refresh

## Endpoints used

- `GET /api/v1/merchandising/products`
- `GET /api/v1/search/predictive`
- Existing commerce/account/cart/checkout endpoints already used by storefront routes.

## Key workflows

### Header autocomplete
1. User types query in header search.
2. Debounced call to `/api/v1/search/predictive`.
3. Suggestions are grouped and displayed.
4. Enter/click navigates to `/products?q=...`.

### Large-catalog browse (`/products`)
1. URL params drive state (`q`, `categoryId`, `storeId`, `sort`, `page`, `size`).
2. Backend pagination metadata powers page navigation.
3. Filter state is preserved while changing sort/category/page/size.

### Wishlist
1. User toggles wishlist on product cards/details.
2. Product snapshot is persisted in local storage.
3. Header badge updates via event subscription.
4. `/wishlist` renders saved products and supports remove/clear + add-to-cart.

### Cart badge refresh
1. Cart mutations dispatch a custom `noura:cart-updated` event.
2. Header listens to event and refreshes count only when needed.
3. Avoids route-coupled cart refetching on every navigation.

## Performance and UX improvements
- Debounced predictive search with short-lived in-memory cache.
- Product listing supports larger page sizes for high-volume catalogs.
- Loading skeleton routes for product listing and product details.
- Relative media URLs normalized to backend host for reliable image rendering.

## Permissions and auth
- Wishlist is client-local and does not require auth.
- Cart/order/account operations continue to require customer token auth from existing flows.


# Storefront Noura

Customer-facing Next.js storefront for the Noura platform.

## Setup

```bash
cd apps/storefront-web
cp .env.example .env.local
npm install
npm run dev
```

Default URL: `http://localhost:3001`

Expected backend URL: `http://localhost:8080`

## Backing APIs

The current storefront client targets the commerce API version prefix (`/api/v1`) by default:

- `GET /api/v1/categories/tree`
- `GET /api/v1/products`
- `GET /api/v1/products/{id}`
- `GET /api/v1/products/{id}/inventory`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/account/profile`
- `GET /api/v1/account/addresses`
- `POST /api/v1/account/addresses`
- `DELETE /api/v1/account/addresses/{id}`
- `GET /api/v1/cart`
- `POST /api/v1/cart/items`
- `PUT /api/v1/cart/items/{itemId}`
- `DELETE /api/v1/cart/items/{itemId}`
- `DELETE /api/v1/cart/items` (clear)
- `POST /api/v1/checkout`
- `GET /api/v1/account/orders`
- `POST /api/v1/account/orders/{orderId}/quick-reorder`
- `GET /api/v1/orders/{orderId}/timeline`
- `GET /api/v1/search/predictive`
- `GET /api/v1/merchandising/products` (supports `query`, `categoryId`, `storeId`, `sort`, `page`, `size`)

## Frontend routes

- `/` homepage + catalog highlights
- `/products` catalog browse
- `/products/[id]` product detail
- `/auth/register` create customer account
- `/auth/login` customer sign-in (stores access token in browser)
- `/cart` cart add/remove/update/checkout
- `/orders` customer order history
- `/orders/[id]` order detail
- `/account/addresses` customer shipping address management
- `/wishlist` client-side wishlist management

The storefront is branded as `Noura` and is intended to remain separate from the internal admin workflows.

## Storefront optimization notes

This storefront now includes:

- Header predictive search with debounced `/search/predictive` integration.
- URL-driven large-catalog controls on `/products`:
  - stable query/category/store/sort/page/page-size state
  - server pagination metadata usage (`totalElements`, `totalPages`)
  - preserved filter context across page transitions
- Wishlist flow (local persistent snapshots, nav badge, product-card toggles, dedicated page).
- Cart badge refresh decoupled from route changes using cart mutation events.
- Product/detail loading skeleton routes:
  - `app/products/loading.jsx`
  - `app/products/[id]/loading.jsx`
- Media URL normalization for backend-hosted relative asset paths.

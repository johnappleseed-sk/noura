# Storefront Noura

Customer-facing Next.js storefront for the Noura platform.

## Setup

```bash
cd frontend/storefront-noura
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

The storefront is branded as `Noura` and is intended to remain separate from the internal admin workflows.

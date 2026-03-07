# POS Storefront Next

Customer-facing storefront scaffold for the POS-to-e-commerce migration.

## Setup

```bash
cd storefront-next
cp .env.example .env.local
npm install
npm run dev
```

Default URL: `http://localhost:3001`

Expected backend URL: `http://localhost:8080`

## Backing APIs

- `GET /api/storefront/v1/catalog/categories`
- `GET /api/storefront/v1/catalog/products`
- `GET /api/storefront/v1/catalog/products/{id}`
- `GET /api/storefront/v1/catalog/products/{id}/availability`
- `POST /api/storefront/v1/customers/register`
- `POST /api/storefront/v1/customers/login`
- `GET /api/storefront/v1/customers/me`
- `GET /api/storefront/v1/customers/me/addresses`
- `POST /api/storefront/v1/customers/me/addresses`
- `DELETE /api/storefront/v1/customers/me/addresses/{id}`
- `GET /api/storefront/v1/cart`
- `POST /api/storefront/v1/cart/items`
- `PATCH /api/storefront/v1/cart/items/{itemId}`
- `DELETE /api/storefront/v1/cart/items/{itemId}`
- `DELETE /api/storefront/v1/cart`
- `POST /api/storefront/v1/orders/checkout` (`shippingAddressId` optional)
- `POST /api/storefront/v1/orders/checkout` (`shippingAddressId`, `paymentMethod`, `paymentProvider`, `paymentProviderReference` optional)
- `GET /api/storefront/v1/orders/me`
- `GET /api/storefront/v1/orders/{orderId}`
- `GET /api/storefront/v1/orders/{orderId}/payments`
- `POST /api/storefront/v1/orders/{orderId}/payments`
- `POST /api/storefront/v1/orders/{orderId}/payments/{paymentId}/capture`

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

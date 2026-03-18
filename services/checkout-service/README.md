# checkout-service

Checkout orchestration service for preview, validation, and synchronous place-order flows.

## Exposed endpoints

- `POST /api/v1/checkout` (legacy alias: `/api/checkout`)
- `POST /api/v1/checkout/preview` (legacy alias: `/api/checkout/preview`)
- `POST /api/v1/checkout/validate` (legacy alias: `/api/checkout/validate`)
- `POST /api/v1/checkout/place-order` (legacy alias: `/api/checkout/place-order`)

## Business scope (v1)

- Synchronous orchestration with downstream services:
  - cart-service
  - customer-service
  - pricing-service
  - inventory-service
  - order-service
  - payment-service
  - notification-service (best effort)
- Deterministic place-order with idempotency key support across checkout, order creation, and payment intent creation.
- Reservation rollback when order creation or payment confirmation fails.
- Internal order finalization to `PAID` after payment reaches `AUTHORIZED` or `CAPTURED`.
- Internal customer lookup so notification-service receives the correct UUID-based target user ID.

## Happy-path orchestration

1. Load cart, customer address, price, and inventory context.
2. Validate each line and compute immutable checkout totals.
3. Reserve inventory for each checkout line.
4. Create the order in `PAYMENT_PENDING`.
5. Create and confirm a payment intent through `payment-service`.
6. Finalize the order to `PAID` through the internal order lifecycle endpoint.
7. Clear the cart and dispatch the order notification on a best-effort basis.

If payment fails or remains non-terminal, checkout releases reservations, cancels the order, and returns a stable failure response.

## Direct checkout payload

`POST /api/v1/checkout` accepts the storefront-compatible direct payload:

```json
{
  "storeId": "11111111-1111-1111-1111-111111111111",
  "addressId": "22222222-2222-2222-2222-222222222222",
  "paymentMethod": "CASH_ON_DELIVERY",
  "paymentProvider": "mock",
  "paymentProviderReference": "storefront-reference",
  "paymentAutoCapture": true,
  "couponCode": "WELCOME10",
  "idempotencyKey": "checkout-123"
}
```

## Idempotency strategy

- Clients may pass idempotency key using either:
  - `Idempotency-Key` header, or
  - request body `idempotencyKey`
- Keys are scoped per customer and persisted in `checkout_request_records`.
- Successful responses are replayed without re-running stock reservations.
- Checkout derives a payment-scoped idempotency key (`{checkoutKey}:payment`) so payment intent creation stays aligned with the checkout command.

## Local run

```bash
cd services/checkout-service
mvn spring-boot:run
```

Required environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Optional environment variables:

- `SERVER_PORT` (default `8080`)
- `CART_SERVICE_BASE_URL` (default `http://localhost:8088`)
- `CUSTOMER_SERVICE_BASE_URL` (default `http://localhost:8089`)
- `PRICING_SERVICE_BASE_URL` (default `http://localhost:8087`)
- `INVENTORY_SERVICE_BASE_URL` (default `http://localhost:8086`)
- `ORDER_SERVICE_BASE_URL` (default `http://localhost:8090`)
- `ORDER_SERVICE_INTERNAL_API_KEY`
- `PAYMENT_SERVICE_BASE_URL` (default `http://localhost:8092`)
- `CUSTOMER_SERVICE_INTERNAL_API_KEY`
- `NOTIFICATION_SERVICE_BASE_URL` (default `http://localhost:8083`)
- `NOTIFICATION_SERVICE_INTERNAL_API_KEY`

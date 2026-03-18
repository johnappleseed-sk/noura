# Purchase Flow

## Happy Path

Text flow:

1. Storefront browses catalog and resolves product details, price, and availability through `catalog-service`, `pricing-service`, and `inventory-service` behind `api-gateway`.
2. Storefront writes cart state through `cart-service`.
3. Storefront validates delivery eligibility and submits direct checkout with `storeId`, `addressId`, typed payment fields, and an idempotency key.
4. `checkout-service` loads cart, customer address, price, and inventory state.
5. `checkout-service` reserves stock in `inventory-service`.
6. `checkout-service` creates the order in `order-service` with status `PAYMENT_PENDING`.
7. `checkout-service` creates and confirms a payment intent in `payment-service`.
8. When payment returns `AUTHORIZED` or `CAPTURED`, `checkout-service` finalizes the order to `PAID` through `POST /internal/orders/{orderId}/status`.
9. `checkout-service` resolves the internal customer UUID through `customer-service`.
10. `checkout-service` clears the cart and dispatches an order notification through `notification-service` on a best-effort basis.
11. Storefront redirects the customer to order history.

## Ownership Boundaries

- `checkout-service` owns orchestration, idempotency coordination, and rollback decisions.
- `order-service` owns order identity, totals, and order lifecycle state.
- `payment-service` owns provider abstraction, payment lifecycle state, and webhook deduplication.
- `inventory-service` owns stock availability and reservation state.
- `customer-service` owns address truth and customer profile identity.
- `notification-service` owns notification persistence and dispatch.

## Idempotency Model

- Storefront generates a checkout idempotency key for direct checkout.
- `checkout-service` persists request/response state in `checkout_request_records`.
- `checkout-service` derives a payment-scoped key by suffixing the checkout key with `:payment`.
- `order-service` and `payment-service` both replay existing records for the same idempotent create command.

## Failure Handling

- Validation failures stop before stock reservation.
- Order-creation failure triggers reservation release.
- Payment failure or non-complete payment state triggers reservation release and a best-effort order cancellation.
- Cart clear and notification dispatch are best effort after a successful paid order so post-order side effects do not mask order completion.

## Current Gaps

- Shipping creation is not yet part of the checkout orchestration path.
- Order-service finalization does not yet persist a payment reference update back onto the order aggregate.
- There is no outbox or saga compensation yet for the case where payment succeeds but order finalization fails and needs asynchronous recovery.

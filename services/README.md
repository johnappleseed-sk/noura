# Services

Canonical service targets:

- `catalog-service`
- `search-service`
- `pricing-service`
- `cart-service`
- `checkout-service`
- `order-service`
- `payment-service`
- `inventory-service`
- `customer-service`
- `promotion-service`
- `shipping-service`
- `notification-service`
- `review-service`

Current state:
- `notification-service` is extracted and runnable.
- `catalog-service` is extracted for read APIs and routes through `api-gateway`.
- `search-service` is extracted for predictive discovery APIs and routes through `api-gateway`.
- `inventory-service` is extracted for stock visibility and stock mutation APIs.
- `pricing-service` is extracted for product price upsert and storefront/checkout price resolution.
- `cart-service` is extracted for persistent carts with catalog/pricing/inventory validation.
- `customer-service` is extracted for storefront customer profile/address flows.
- `order-service` is extracted for order aggregate creation/history/status.
- `checkout-service` is extracted for preview/validation/place-order orchestration.
- `payment-service` is extracted for internal payment intent lifecycle, sandbox provider orchestration, and webhook-ready status handling.
- Other services are extraction targets reusing logic from `archive/legacy-monolith/backend-monolith`.

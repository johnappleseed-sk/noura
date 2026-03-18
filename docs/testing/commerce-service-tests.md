# Commerce Service Tests

This test pass adds pragmatic automated coverage for the most business-critical commerce paths without introducing a brittle full-environment suite.

## Coverage Scope

- `pricing-service`
  - product price lookup and store-specific resolution
- `promotion-service`
  - promo-code validation, deterministic discount evaluation, and admin safety checks
- `cart-service`
  - add item, update quantity, remove item, and cart envelope behavior
- `inventory-service`
  - reserve, release, deduct guardrails, and low-stock lookup
- `checkout-service`
  - checkout validation and place-order happy path through the HTTP controller boundary with the real orchestration service
- `order-service`
  - idempotent create behavior, initial status-history persistence, valid status updates, and quick reorder
- `payment-service`
  - create intent, confirm/capture flow, and webhook idempotency

## Test Strategy

- Use unit tests for deterministic domain and lifecycle rules.
- Use Web MVC slice tests for HTTP envelopes and request validation.
- Use a controller-plus-service slice for checkout so the request contract and orchestration logic are exercised together without requiring a full multi-service runtime.
- Keep downstream integrations mocked in service/controller tests to avoid environment-coupled failures and to keep fixtures realistic but stable.

## Commands

Run the targeted commerce suites with:

```bash
cd services/pricing-service && mvn -q test
cd services/promotion-service && mvn -q test
cd services/cart-service && mvn -q test
cd services/inventory-service && mvn -q test
cd services/checkout-service && mvn -q test
cd services/order-service && mvn -q test
cd services/payment-service && mvn -q test
```

## Current Limits

- There is still no containerized gateway-to-services end-to-end suite.
- Repository-slice testing remains intentionally light; the current signal comes from domain/service tests and Web MVC contract tests.
- Checkout happy-path coverage is synchronous and mocked at service boundaries, which is appropriate for the current extracted architecture but not a substitute for deployed smoke tests.

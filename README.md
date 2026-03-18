# Noura Commerce Platform

Enterprise commerce platform monorepo with canonical app, service, package, platform, archive, and docs roots.

## Platform Highlights
- Persistent RBAC governance (roles, permissions, role assignments) with admin management UI.
- Enterprise module-based RBAC catalog (Dashboard, Catalog, Inventory, Orders, Customers, Sales & Promotions, Finance, Marketing, Support, Vendors, Analytics, Shipping, Localization, Procurement).
- RBAC permission presets and bulk user-role assignment workflows for faster governance operations.
- RBAC mutation audit logging with backend query API and admin UI visibility.
- RBAC audit filtering and CSV export workflow for compliance and operations handoff.
- RBAC audit integrity hashing (`payload_hash`) with database-level immutability controls.
- Capability-driven admin routing/navigation backed by `/api/v1/admin/capabilities`.
- Versioned RBAC matrix contract for governance diagnostics.
- Extracted `promotion-service` with deterministic promo-code validation, cart discount evaluation, and admin promotion CRUD.
- Extracted `payment-service` with internal payment intents, sandbox provider abstraction, and webhook-ready event deduplication.
- Extracted `shipping-service` with rule-based shipping quotes, shipment records, and carrier-ready fulfillment status hooks.
- Extracted `review-service` with moderated storefront reviews, approved-only rating aggregation, and admin approval/rejection actions.
- Extracted `search-service` as the projection-backed discovery boundary for product search, autocomplete, trend tags, and provider-ready indexing.
- Cross-service integration hardening: gateway routes now expose payment/shipping APIs, cart-service coupon flows integrate with promotion-service validation, and notification-service aligns to the shared response/correlation contract.
- Complete synchronous storefront purchase flow: cart -> checkout validation -> stock reservation -> order creation -> payment confirmation -> order finalization -> notification dispatch.
- Frontend contract alignment: extracted services now cover the active storefront/admin compatibility surface for recommendations, merchandising controls, payment methods, quick reorder, checkout steps, merchants, stores, and service areas without reviving the legacy catch-all `app-service`.
- Pragmatic commerce test baseline across pricing, promotion, cart, inventory, checkout, order, and payment critical flows.

## Repository Modules
- `apps/admin-web/` React admin operations console
- `apps/storefront-web/` Next.js storefront
- `apps/api-gateway/` Spring Cloud Gateway
- `services/` domain microservice targets
- `packages/` shared cross-service modules
- `platform/` infra-as-code and local platform bootstrap
- `archive/` retained legacy code and compatibility artifacts
- `docs/` architecture, API, feature, and onboarding documentation

Legacy code retained for reuse:
- `archive/legacy-monolith/backend-monolith/` (current monolith codebase used during extraction)
- `archive/legacy-monolith/mobile-app/` (mobile code retained; not part of active app root)

## Key Documentation
- Onboarding: [docs/onboarding.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/onboarding.md)
- Changelog: [docs/CHANGELOG.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/CHANGELOG.md)
- Backend API notes: [docs/backend-api.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/backend-api.md)
- Frontend API audit: [docs/frontend-api-audit.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/frontend-api-audit.md)
- RBAC architecture: [docs/architecture/rbac-foundation.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/architecture/rbac-foundation.md)
- RBAC feature guide: [docs/features/rbac-authorization-matrix.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/features/rbac-authorization-matrix.md)
- RBAC API: [docs/api/admin-authorization-matrix.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/admin-authorization-matrix.md)
- RBAC database model: [docs/database/rbac-modeling.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/database/rbac-modeling.md)
- Service persistence standards: [docs/database/service-persistence-standards.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/database/service-persistence-standards.md)
- Enterprise RBAC feature: [docs/features/enterprise-rbac.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/features/enterprise-rbac.md)
- RBAC admin runbook: [docs/operations/rbac-admin-runbook.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/operations/rbac-admin-runbook.md)
- Payment API: [docs/api/payment-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/payment-service.md)
- Payment architecture: [docs/architecture/payment-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/architecture/payment-service.md)
- Promotion API: [docs/api/promotion-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/promotion-service.md)
- Promotion architecture: [docs/architecture/promotion-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/architecture/promotion-service.md)
- Review API: [docs/api/review-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/review-service.md)
- Review architecture: [docs/architecture/review-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/architecture/review-service.md)
- Search API: [docs/api/search-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/search-service.md)
- Search architecture: [docs/architecture/search-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/architecture/search-service.md)
- Shipping API: [docs/api/shipping-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/shipping-service.md)
- Shipping architecture: [docs/architecture/shipping-service.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/architecture/shipping-service.md)
- Purchase flow architecture: [docs/architecture/purchase-flow.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/architecture/purchase-flow.md)
- Commerce test coverage: [docs/testing/commerce-service-tests.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/testing/commerce-service-tests.md)
- API error response standard: [docs/api/error-response-standard.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/api/error-response-standard.md)

## Local Development
### Legacy monolith backend (during migration)
```bash
cd archive/legacy-monolith/backend-monolith
./mvnw spring-boot:run
```

### Admin dashboard
```bash
cd apps/admin-web
npm install
npm run dev
```

### Storefront
```bash
cd apps/storefront-web
npm install
npm run dev
```

### Platform local stack
```bash
cd platform/scripts
cp .env.example .env
docker compose -f docker-compose.local.yml up -d --build
```

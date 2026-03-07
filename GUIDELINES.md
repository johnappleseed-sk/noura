# NOURA Guidelines

This file is a compact reference for future code changes and reviews in this repository.

## Scope

- Backend: Spring Boot API in `backend/`
- Frontend: Vite + React admin app in `frontend/`
- Infra: local dev stack in `docker-compose.yml`

## Non-Negotiables

- Never commit real secrets.
- Never ship default JWT secrets to shared environments.
- Never log sensitive auth data (password-reset tokens, access tokens, refresh tokens).
- Keep authorization checks explicit on privileged endpoints.
- Keep pricing and discount logic consistent across cart and checkout.

## Security Rules

- JWT:
  - Read secret from environment only.
  - Reject weak/default secrets at startup in non-local environments.
- Password reset:
  - Do not log reset tokens.
  - Prefer hashed reset tokens at rest if practical.
- CORS:
  - Restrict to known origins per environment.
- Rate limiting:
  - Do not rely only on raw `remoteAddr` in proxy deployments.
  - Use trusted forwarded headers and bounded storage (TTL/eviction).

## Backend Coding Guidelines

- Keep business rules in services, not controllers.
- Use one source of truth for coupons/discounts (DB-backed preferred).
- Validate ownership checks on user-scoped resources (cart, orders, notifications, etc.).
- Keep transactional boundaries on write flows.
- Add/maintain tests for:
  - auth and role-protected behavior
  - cart/checkout totals
  - stock validation
  - notification read/write flows

## Frontend Guidelines

- Keep path aliases consistent across:
  - `vite.config.ts`
  - `tsconfig*.json`
  - `jest.config.cjs`
- If `src/enterprise` tests are active, ensure aliases and includes target enterprise paths.
- Keep API client auth handling centralized in one place.
- Protect role-restricted routes in router and UI state.

## Review Checklist (Quick Pass)

- Security:
  - secrets, auth, token logging, access control
- Data integrity:
  - totals, discounts, stock decrements, idempotency
- Reliability:
  - null/empty handling, transaction safety, race-prone code
- Test coverage:
  - changed paths covered by unit/integration tests
- Build health:
  - backend tests pass
  - frontend typecheck/build pass
  - frontend tests pass (or failures are tracked intentionally)

## Local Verification Commands

- Backend:
  - `mvn test -q`
- Frontend:
  - `npm run typecheck`
  - `npm run build`
  - `npm test -- --runInBand`

## Current Known Gaps (as of 2026-03-04)

- Rate limiting is in-memory only (not shared across app instances); use Redis-backed limiter for multi-instance deployments.
- Frontend production bundle has a large main chunk warning (>500 kB); add code-splitting/manual chunks.
- Pricing currently supports one coupon code per checkout flow; multi-coupon stacking is not implemented.

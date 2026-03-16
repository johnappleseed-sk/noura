# Noura Mobile App

Enterprise-grade Flutter mobile commerce client for the Noura platform.

## Stack
- Flutter (Dart 3)
- Riverpod (`AsyncNotifier`/`FutureProvider`)
- GoRouter
- Dio + interceptors (auth refresh, retry, logging)
- Secure token storage (`flutter_secure_storage`)

## Architecture
Feature-first layered structure:
- `presentation`: screens/widgets
- `application`: Riverpod controllers/state
- `domain`: entities + repository contracts
- `data`: repository implementations + DTOs
- `core`: config, network, error handling, shared widgets/utilities

Main feature modules:
- `auth`
- `commerce`
- `shopping`
- `wishlist`
- `account`

## Environment Configuration
Runtime API configuration uses compile-time defines:

- `APP_ENV` (default: `dev`)
- `API_BASE_URL` (default: `http://localhost:8080`)
- `API_VERSION_PATH` (default: `/api/v1`)
- `API_CONNECT_TIMEOUT_MS` (default: `15000`)
- `API_SEND_TIMEOUT_MS` (default: `15000`)
- `API_RECEIVE_TIMEOUT_MS` (default: `20000`)
- `API_ENABLE_LOGS` (default: `true` in non-release)

Example:

```bash
flutter run \
  --dart-define=APP_ENV=dev \
  --dart-define=API_BASE_URL=http://localhost:8080 \
  --dart-define=API_VERSION_PATH=/api/v1
```

## Implemented Flows
- Auth: login/register/forgot/reset + session persistence
- Auth hardening: automatic logout state propagation when refresh token flow fails
- Commerce: home feed, categories, product list/detail, search, reviews/ratings
- Shopping: wishlist, cart, addresses, checkout, order detail
- Account: profile, orders, notifications, settings, support, legal pages

## Current API Notes
- Wishlist currently uses local secure persistence for product IDs and hydrates
  items through product-detail API.
- CMS/legal content endpoints are not exposed yet. Terms/privacy/about are
  normalized from runtime message keys with deterministic fallback content.

## Quality Gates
Before committing:

```bash
flutter analyze
flutter test
```

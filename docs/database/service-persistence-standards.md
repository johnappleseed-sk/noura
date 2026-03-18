# Service Persistence Standards

## Purpose

This document records the current database conventions for extracted NOURA services so new services do not drift into incompatible schema patterns.

## Current standards

- Each service that owns transactional state should own a Flyway migration set under `src/main/resources/db/migration`.
- Each service should use its own Flyway history table in `application.yml`.
- JPA entity validation should use `spring.jpa.hibernate.ddl-auto=validate` for service-owned schemas.
- `created_at` and `updated_at` should be `TIMESTAMPTZ NOT NULL` columns. New migrations should prefer `DEFAULT NOW()` even when the application also sets timestamps.
- `created_by` and `updated_by` should be present when the service has a stable actor concept. If a service is primarily system-driven, timestamp audit may be sufficient until actor semantics are defined explicitly.
- Enums must be stored as strings. Do not use ordinal enum persistence.
- Cross-service foreign keys are not allowed.
- Intra-service ownership relationships should use foreign keys when the referenced aggregate is service-owned and the lifecycle is not intentionally soft-linked.
- UUID is the default primary key type for service-owned transactional tables.

## Concurrency model

- The current platform does not use JPA `@Version`.
- Services that need write serialization currently rely on idempotency keys and selective pessimistic locking.
- Do not mix optimistic locking into one service casually without an explicit concurrency design decision.

## 2026-03-18 cleanup outcomes

The persistence cleanup standardized the following:

- Added missing Flyway-owned runtime tables:
  - `customer_payment_methods`
  - `legacy_price_lists`
- Added missing intra-service FK:
  - `store_records.merchant_id -> merchant_records.id`
- Added hot-path lookup indexes for:
  - inventory stock and movement history
  - notification unread-count reads
  - payment latest-by-order queries
  - review product timelines
  - shipping case-insensitive network code lookups
- Standardized DB-managed timestamp defaults on:
  - `notification_messages`
  - `promotions`
  - `promotion_applications`
  - `product_reviews`
  - `shipment_records`
  - `merchant_records`
  - `store_records`
  - `service_area_records`

## Explicit exception

`catalog-service` is intentionally read-only over shared catalog tables and does not own a Flyway migration set yet. That exception should remain explicit until catalog write ownership is extracted into a service-owned schema plan.

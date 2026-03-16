# Product Sync Bridge

## Overview
Product Sync Bridge connects commerce products to inventory products and provides durable asynchronous mirroring for generator-enriched fields.

## Business Rules
- Commerce product is canonical for generated description, barcode, and QR code values.
- Inventory product is mirror-only for these fields.
- Sync is eventually consistent through queued mirror jobs.
- Missing mapping does not fail canonical generation; job is marked blocked.

## Architecture
Tables:
- `product_generator_bridge`
  - `commerce_product_id` (UUID, unique)
  - `inventory_product_id` (string UUID, unique)
- `product_generator_mirror_jobs`
  - status, attempts, retry metadata, json payload

Runtime components:
- Job enqueue in `ProductEnrichmentServiceImpl`
- Scheduled consumer in `ProductMirrorSyncWorker`
- Inventory write adapter in `ProductInventoryMirrorService`

Status lifecycle:
- `PENDING` -> `SYNCED`
- `PENDING`/`RETRYING` -> `FAILED` (max attempts reached)
- `PENDING`/`RETRYING` -> `BLOCKED_MAPPING` (bridge missing)

## Backend
Job payload keys:
- `description`
- `barcode`
- `qrCode`

Mirror worker behavior:
- Loads due jobs by status and retry time.
- Resolves bridge mapping for commerce product.
- Updates inventory product fields in inventory transaction manager.
- Applies exponential backoff retries.

## Frontend
- Search results include `mirror_status`.
- Generation responses include `mirror_status` and optional `mirror_warning`.
- Admin page shows warning banner when mapping is missing.

## Configuration
Mirror controls:
- `APP_PRODUCT_GENERATOR_MIRROR_MAX_ATTEMPTS`
- `APP_PRODUCT_GENERATOR_MIRROR_BATCH_SIZE`
- `APP_PRODUCT_GENERATOR_MIRROR_RETRY_BASE_SECONDS`
- `APP_PRODUCT_GENERATOR_MIRROR_WORKER_DELAY_MS`

## Usage Example
1. Generate barcode for product in commerce API.
2. Service writes barcode to commerce product and creates mirror job.
3. Worker picks up job and writes `barcodeValue` in inventory product.
4. Job status transitions to `SYNCED`.

## Edge Cases
- Bridge missing: `BLOCKED_MAPPING` + warning returned to caller.
- Inventory product deleted/missing: retries until max attempts, then `FAILED`.
- Empty payload fields are ignored in inventory update.

## Notes
- This bridge currently mirrors only generator fields.
- Additional field mirroring should reuse same job infrastructure.

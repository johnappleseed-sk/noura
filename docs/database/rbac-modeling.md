# RBAC Database Modeling Notes

## Overview
Persistent RBAC storage is implemented by migration:
- `backend/src/main/resources/db/migration/V20__admin_rbac_persistent_storage.sql`
- `backend/src/main/resources/db/migration/V21__admin_rbac_audit_logs.sql`
- `backend/src/main/resources/db/migration/V22__admin_rbac_audit_integrity_controls.sql`
- `backend/src/main/resources/db/migration/V24__admin_rbac_bulk_assignment_saved_views.sql`

This migration introduces normalized role, permission, role-permission, and user-role tables and seed data.

## Tables
### `admin_roles`
- `id` UUID PK
- `code` unique role code
- `label`
- `description`
- `is_system_role`
- `assignable`
- `active`
- audit columns inherited by convention (`created_at`, `updated_at`, `created_by`)

### `admin_permissions`
- `id` UUID PK
- `scope` (domain boundary, e.g. `products`, `orders`)
- `action` (`read`, `create`, `update`, `delete`, `approve`, `export`)
- `label`
- `description`
- unique constraint on `(scope, action)`

### `admin_role_permissions`
- composite PK: `(role_id, permission_id)`
- FK `role_id -> admin_roles.id`
- FK `permission_id -> admin_permissions.id`

### `admin_user_roles`
- composite PK: `(user_id, role_id)`
- FK `user_id -> users.id`
- FK `role_id -> admin_roles.id`

### `admin_rbac_audit_logs`
- `id` UUID PK
- `action_type`
- `entity_type`
- `entity_id`
- `actor_email`
- `actor_user_id` (nullable FK to `users.id`)
- `outcome`
- `correlation_id`
- `payload_hash` (SHA-256 integrity hash of canonical payload tuple)
- `details_json`
- `occurred_at`
- audit columns (`created_at`, `updated_at`, `created_by`)

### `admin_bulk_user_role_views`
- `id` UUID PK
- `owner_user_id` FK -> `users.id`
- `name` (max 120)
- `query_text` (max 255)
- `user_ids_json` (JSON-encoded UUID array)
- `role_codes_json` (JSON-encoded role code array)
- audit columns (`created_at`, `updated_at`, `created_by`)

## Relationships
- One `admin_role` has many `admin_role_permissions`.
- One `admin_permission` can be assigned to many roles.
- One `user` can have many admin roles through `admin_user_roles`.
- RBAC mutations produce immutable rows in `admin_rbac_audit_logs`.

## Indexes and Constraints
- `uk_admin_permissions_scope_action` on `(scope, action)`
- `idx_admin_roles_code`
- `idx_admin_roles_active`
- `idx_admin_permissions_scope`
- `idx_admin_user_roles_role`
- `idx_admin_rbac_audit_logs_occurred_at`
- `idx_admin_rbac_audit_logs_action`
- `idx_admin_rbac_audit_logs_entity`
- `idx_admin_rbac_audit_logs_actor`
- `uk_admin_bulk_user_role_views_owner_name` unique on `(owner_user_id, lower(name))`
- `idx_admin_bulk_user_role_views_owner`
- FK cascades:
  - deleting a role deletes role grants and user-role assignments
  - deleting a permission deletes role-permission assignments
  - deleting a user deletes user-role assignments
  - deleting a user deletes saved bulk-assignment views they own
- Integrity/immutability controls (V22):
  - backfills existing audit rows with computed SHA-256 `payload_hash`
  - enforces `payload_hash` `NOT NULL`
  - `BEFORE UPDATE` trigger blocks mutation attempts
  - `BEFORE DELETE` trigger blocks deletion attempts

## Seed Data
- Permission catalog seeded for enterprise scopes:
  - `products`, `categories`, `media`, `orders`, `customers`, `users`, `staff`, `roles`, `inventory`, `discounts`, `reviews`, `reports`, `settings`, `integrations`, `audit_logs`
- Action catalog seeded:
  - `read`, `create`, `update`, `delete`, `approve`, `export`
- Default roles seeded:
  - `SUPER_ADMIN`, `ADMIN`, `MANAGER`, `CONTENT_MANAGER`, `PRODUCT_MANAGER`, `INVENTORY_MANAGER`, `ORDER_MANAGER`, `SUPPORT_AGENT`, `FINANCE`, `ANALYST`, `STAFF`, `WAREHOUSE_MANAGER`, `VIEWER`, `CUSTOMER`, `B2B`
- Grants seeded:
  - full grants for `SUPER_ADMIN` and `ADMIN`
  - curated grants for other enterprise roles
- Backfill:
  - legacy `ADMIN` users in `user_roles` are mirrored into `admin_user_roles`

## Operational Notes
- Role deactivation keeps role row but sets `active=false` and removes user-role assignments.
- System roles are seeded with `is_system_role=true`.
- Role and assignment mutations are audit-logged with actor + correlation id + JSON details payload.
- Saved bulk-assignment views are actor-scoped and enforce case-insensitive uniqueness per actor (`lower(name)`).
- Saved-view upsert and delete operations are audit-logged as RBAC governance events.
- Audit exports include `payload_hash` so downstream compliance systems can validate row-level integrity.

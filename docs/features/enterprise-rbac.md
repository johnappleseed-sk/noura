# Enterprise RBAC Module

## Overview

The admin RBAC module now supports enterprise-grade role governance for e-commerce operations.

- Role CRUD for system and custom roles
- Module-grouped permission catalog
- Role-permission replacement workflows
- User-role assignment workflows
- Bulk assignment preview and actor-scoped saved views
- RBAC audit logs for change traceability

## Supported permission modules

- Dashboard
- Catalog
- Inventory
- Orders
- Customers
- Sales & Promotions
- Finance
- Marketing
- Support
- Vendors
- Analytics
- Shipping
- Localization
- Procurement

## Default enterprise roles

- Super Admin
- Admin
- Store Manager
- Product Manager
- Inventory Manager
- Finance Officer
- Customer Support
- Marketing Manager

Legacy role codes remain supported for backward compatibility (`MANAGER`, `SUPPORT_AGENT`, `FINANCE`, etc.).

## Backend APIs

RBAC APIs are served under:

- `GET /api/v1/admin/authorization/permissions`
- `GET /api/v1/admin/authorization/roles`
- `POST /api/v1/admin/authorization/roles`
- `PATCH /api/v1/admin/authorization/roles/{roleId}`
- `PUT /api/v1/admin/authorization/roles/{roleId}/permissions`
- `DELETE /api/v1/admin/authorization/roles/{roleId}`
- `GET /api/v1/admin/authorization/users/{userId}/roles`
- `PUT /api/v1/admin/authorization/users/{userId}/roles`
- `POST /api/v1/admin/authorization/users/roles/bulk/preview`
- `PUT /api/v1/admin/authorization/users/roles/bulk`
- `GET /api/v1/admin/authorization/users/roles/bulk/views`
- `POST /api/v1/admin/authorization/users/roles/bulk/views`
- `DELETE /api/v1/admin/authorization/users/roles/bulk/views/{viewId}`
- `GET /api/v1/admin/authorization/audit-logs`
- `GET /api/v1/admin/authorization/matrix`

## Storage and seed strategy

- Migration: `V23__enterprise_rbac_module_catalog.sql`
  - Adds enterprise permission metadata (`module_group`, `display_order`, `is_sensitive`)
  - Adds indexes and constraints for performant governance queries
  - Seeds baseline enterprise modules, roles, and grants
- Migration: `V24__admin_rbac_bulk_assignment_saved_views.sql`
  - Adds actor-scoped persistent saved views for bulk user-role assignment workflows

- Runtime bootstrap seeder: `AdminRbacReferenceDataSeeder`
  - Idempotently ensures reference permissions/roles/grants exist
  - Preserves existing custom role behavior
  - Automatically maps legacy platform `ADMIN` users to `ADMIN` RBAC role assignments

## Frontend UX

`RolesPermissionsPage` now renders grouped permission cards with:

- Global select-all
- Per-module select-all
- Save/update role permissions
- Role metadata editor
- User assignment panel
- Audit-log viewer

## Operations

Production operating procedures are documented in
[docs/operations/rbac-admin-runbook.md](/Users/Saturn/Downloads/Coding/Projects/noura/docs/operations/rbac-admin-runbook.md).

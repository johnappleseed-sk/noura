# Noura Enterprise Commerce — API Endpoint Analysis

> Generated from OpenAPI 3.0.1 spec (`/v3/api-docs`) cross-referenced against all 19 admin-dashboard endpoint files, 21 page components, and the Control Center workbench.

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Platform API — All Endpoints](#2-platform-api--all-endpoints)
3. [Inventory API — All Endpoints](#3-inventory-api--all-endpoints)
4. [Admin Dashboard Usage Matrix](#4-admin-dashboard-usage-matrix)
5. [Unused Backend Endpoints (Gaps)](#5-unused-backend-endpoints-gaps)
6. [Orphan Frontend Exports](#6-orphan-frontend-exports)
7. [Improvement Recommendations](#7-improvement-recommendations)
8. [Security Audit](#8-security-audit)
9. [Endpoint Optimization Guidance](#9-endpoint-optimization-guidance)

---

## 1. Executive Summary

| Metric | Count |
|---|---|
| Total backend endpoints (unique paths × methods) | **~120** |
| Platform API endpoints (`/api/v1/`) | **~80** |
| Inventory API endpoints (`/api/inventory/v1/`) | **~40** |
| Frontend endpoint files | **19** |
| Frontend exported functions | **73** |
| Admin dashboard pages/routes | **21** |
| Endpoints actively used by admin dashboard | **~65** |
| Endpoints NOT used by admin dashboard | **~55** |
| Storefront-only endpoints (cart, checkout, account) | **~25** |
| Admin-relevant but unused endpoints | **~30** |
| Orphan frontend exports (never imported by pages) | **4** |

---

## 2. Platform API — All Endpoints

### 2.1 Auth Controller (`auth-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 1 | POST | `/api/v1/auth/login` | Authenticate user, return JWT tokens | ✅ `authApi.loginPassword` → LoginPage |
| 2 | POST | `/api/v1/auth/register` | Register new user account | ⚠️ Exported but unused in admin |
| 3 | POST | `/api/v1/auth/refresh` | Refresh expired access token | ❌ Not used |
| 4 | POST | `/api/v1/auth/password-reset/request` | Request password reset email | ❌ Not used |
| 5 | POST | `/api/v1/auth/password-reset/confirm` | Confirm password reset with token | ❌ Not used |

### 2.2 Admin Dashboard Controller (`admin-dashboard-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 6 | GET | `/api/v1/admin/dashboard/summary` | Revenue, orders, users, stores KPIs | ⚠️ Exported (`dashboardApi`) but DashboardPage uses inventory endpoints instead |
| 7 | GET | `/api/v1/admin/users` | Paginated user list | ✅ `adminApi.listAdminUsers` → UsersPage |
| 8 | PATCH | `/api/v1/admin/users/{userId}` | Update user roles/enabled status | ✅ `adminApi.updateAdminUser` → UsersPage |
| 9 | GET | `/api/v1/admin/b2b/approvals` | B2B approval queue | ✅ `adminApi.listApprovalQueue` → ControlCenterPage |
| 10 | PATCH | `/api/v1/admin/b2b/approvals/{approvalId}` | Approve/reject B2B request | ✅ `adminApi.updateApproval` → ControlCenterPage |

### 2.3 Product Controller (`product-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 11 | GET | `/api/v1/products` | List products (paginated, filterable) | ✅ `commerceProductsApi.listCommerceProducts` → CommerceCatalogPage |
| 12 | POST | `/api/v1/products` | Create new product | ✅ `commerceProductsApi.createCommerceProduct` → CommerceCatalogPage |
| 13 | GET | `/api/v1/products/{productId}` | Get single product detail | ✅ `commerceProductsApi.getCommerceProduct` → CommerceCatalogPage |
| 14 | PUT | `/api/v1/products/{productId}` | Full update product | ✅ `commerceProductsApi.updateCommerceProduct` → CommerceCatalogPage |
| 15 | PATCH | `/api/v1/products/{productId}` | Partial update (descriptions, SEO, flags) | ✅ `commerceProductsApi.patchCommerceProduct` → CommerceCatalogPage |
| 16 | DELETE | `/api/v1/products/{productId}` | Delete product | ✅ `commerceProductsApi.deleteCommerceProduct` → CommerceCatalogPage |
| 17 | GET | `/api/v1/products/{productId}/variants` | List product variants | ❌ Not used |
| 18 | POST | `/api/v1/products/{productId}/variants` | Add variant to product | ✅ `commerceProductsApi.addCommerceVariant` → CommerceCatalogPage |
| 19 | GET | `/api/v1/products/{productId}/reviews` | List product reviews | ❌ Not used |
| 20 | POST | `/api/v1/products/{productId}/reviews` | Add review | ❌ Storefront-only |
| 21 | POST | `/api/v1/products/{productId}/media` | Add media to product | ✅ `commerceProductsApi.addCommerceMedia` → CommerceCatalogPage |
| 22 | GET | `/api/v1/products/{productId}/inventory` | List product store inventories | ⚠️ Exported but unused |
| 23 | PUT | `/api/v1/products/{productId}/inventory` | Upsert store inventory | ✅ `commerceProductsApi.upsertCommerceStoreInventory` → CommerceCatalogPage |
| 24 | GET | `/api/v1/products/{productId}/related` | Related products | ❌ Storefront-only |
| 25 | GET | `/api/v1/products/{productId}/frequently-bought-together` | FBT products | ❌ Storefront-only |
| 26 | GET | `/api/v1/products/trend-tags` | Product trend tags | ❌ Not used |

### 2.4 Product Variant Controller (`product-variant-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 27 | PUT | `/api/v1/variants/{variantId}` | Update a variant | ✅ `commerceProductsApi.updateCommerceVariant` → CommerceCatalogPage |

### 2.5 Catalog Management Controller (`catalog-management-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 28 | GET | `/api/v1/categories/tree` | Full category tree (with locale support) | ✅ `commerceCategoriesApi.getCommerceCategoryTree` → CommerceCatalogPage |
| 29 | POST | `/api/v1/categories` | Create category | ✅ `commerceCategoriesApi.createCommerceCategory` → CommerceCatalogPage |
| 30 | PUT | `/api/v1/categories/{categoryId}` | Update category | ✅ `commerceCategoriesApi.updateCommerceCategory` → CommerceCatalogPage |
| 31 | GET | `/api/v1/categories/analytics` | Category analytics (revenue, units, conversion) | ✅ `analyticsApi.getCategoryAnalytics` → AnalyticsPage |
| 32 | PUT | `/api/v1/categories/{categoryId}/translations/{locale}` | Upsert category translation | ❌ Not used |
| 33 | GET | `/api/v1/categories/{categoryId}/translations` | List category translations | ❌ Not used |
| 34 | POST | `/api/v1/categories/channel-mappings` | Create channel-category mapping | ❌ Not used |
| 35 | GET | `/api/v1/categories/{categoryId}/channel-mappings` | List channel mappings | ❌ Not used |
| 36 | GET | `/api/v1/categories/change-requests` | List change requests (PENDING/APPROVED/REJECTED) | ❌ Not used |
| 37 | POST | `/api/v1/categories/change-requests` | Submit category change request | ❌ Not used |
| 38 | PATCH | `/api/v1/categories/change-requests/{requestId}/approve` | Approve change request | ❌ Not used |
| 39 | PATCH | `/api/v1/categories/change-requests/{requestId}/reject` | Reject change request | ❌ Not used |
| 40 | POST | `/api/v1/categories/ai/suggest` | AI-powered category suggestion | ❌ Not used |
| 41 | POST | `/api/v1/attributes` | Create product attribute | ❌ Not used |
| 42 | POST | `/api/v1/attribute-sets` | Create attribute set | ❌ Not used |

### 2.6 Store Controller (`store-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 43 | GET | `/api/v1/stores` | List stores (paginated, filterable) | ✅ `storesApi.listStores` → StoresPage, CommerceCatalogPage |
| 44 | POST | `/api/v1/stores` | Create store | ✅ `storesApi.createStore` → StoresPage |
| 45 | PUT | `/api/v1/stores/{storeId}` | Update store | ✅ `storesApi.updateStore` → StoresPage |
| 46 | DELETE | `/api/v1/stores/{storeId}` | Delete store | ✅ `storesApi.deleteStore` → StoresPage |
| 47 | PUT | `/api/v1/stores/preferred/{storeId}` | Set user's preferred store | ❌ Storefront-only |
| 48 | GET | `/api/v1/stores/nearest` | Find nearest stores by GPS | ❌ Storefront-only |

### 2.7 Order Controller (`order-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 49 | GET | `/api/v1/orders` | List all orders (admin, paginated) | ✅ `ordersApi.listOrders` → OrdersPage, AnalyticsPage, ReturnsPage |
| 50 | GET | `/api/v1/orders/{orderId}` | Get single order detail | ❌ Not used |
| 51 | GET | `/api/v1/orders/{orderId}/timeline` | Order timeline events | ✅ `ordersApi.getOrderTimeline` → OrdersPage |
| 52 | PATCH | `/api/v1/orders/{orderId}/status` | Update order & refund status | ✅ `ordersApi.updateOrderStatus` → OrdersPage, ReturnsPage |

### 2.8 Notification Controller (`notification-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 53 | GET | `/api/v1/notifications/me` | Current user's notifications | ✅ `notificationsApi.listMyNotifications` → NotificationsPage |
| 54 | GET | `/api/v1/notifications/me/unread-count` | Unread notification count | ✅ `notificationsApi.getUnreadCount` → NotificationsPage |
| 55 | PATCH | `/api/v1/notifications/{notificationId}/read` | Mark single read | ✅ `notificationsApi.markNotificationRead` → NotificationsPage |
| 56 | PATCH | `/api/v1/notifications/me/read-all` | Mark all read | ✅ `notificationsApi.markAllNotificationsRead` → NotificationsPage |
| 57 | POST | `/api/v1/notifications/user/{userId}` | Push notification to user | ✅ `notificationsApi.pushNotificationToUser` → NotificationsPage |
| 58 | POST | `/api/v1/notifications/broadcast` | Broadcast to all users | ✅ `notificationsApi.broadcastNotification` → NotificationsPage |

### 2.9 Pricing Controller (`pricing-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 59 | GET | `/api/v1/price-lists` | List price lists | ✅ `pricingApi.listPriceLists` → PricingPage |
| 60 | POST | `/api/v1/price-lists` | Create price list | ✅ `pricingApi.createPriceList` → PricingPage |
| 61 | POST | `/api/v1/prices` | Upsert price entry | ✅ `pricingApi.upsertPrice` → PricingPage |
| 62 | GET | `/api/v1/prices/variants/{variantId}` | Quote variant price | ✅ `pricingApi.quoteVariantPrice` → PricingPage |
| 63 | GET | `/api/v1/promotions/active` | Active promotions list | ✅ `pricingApi.listActivePromotions` → PricingPage |
| 64 | POST | `/api/v1/promotions` | Create promotion | ✅ `pricingApi.createPromotion` → PricingPage |

### 2.10 Inventory Controller (`inventory-controller` — Platform side)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 65 | GET | `/api/v1/inventory/warehouses` | List warehouses (platform) | ❌ Not used (admin uses inventory API) |
| 66 | POST | `/api/v1/inventory/warehouses` | Create warehouse (platform) | ❌ Not used |
| 67 | GET | `/api/v1/inventory/{variantId}` | Variant stock summary | ❌ Not used |
| 68 | GET | `/api/v1/inventory/variants/{variantId}` | Variant stock summary (alt) | ❌ Not used |
| 69 | POST | `/api/v1/inventory/reserve` | Reserve inventory | ❌ Not used (checkout flow) |
| 70 | POST | `/api/v1/inventory/release` | Release reservation | ❌ Not used |
| 71 | POST | `/api/v1/inventory/confirm` | Confirm reservation | ❌ Not used |
| 72 | POST | `/api/v1/inventory/check` | Check availability | ❌ Not used |
| 73 | POST | `/api/v1/inventory/adjust` | Adjust stock | ❌ Not used (admin uses inventory API adjustments) |

### 2.11 Checkout Controller (`checkout-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 74 | POST | `/api/v1/checkout` | Direct checkout | ❌ Storefront-only |
| 75 | POST | `/api/v1/checkout/steps/shipping` | Shipping step | ❌ Storefront-only |
| 76 | POST | `/api/v1/checkout/steps/payment` | Payment step | ❌ Storefront-only |
| 77 | POST | `/api/v1/checkout/steps/confirm` | Confirm step | ❌ Storefront-only |
| 78 | GET | `/api/v1/checkout/steps/review` | Review step | ❌ Storefront-only |

### 2.12 Cart Controller (`cart-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 79 | GET | `/api/v1/cart` | Get current user's cart | ❌ Storefront-only |
| 80 | POST | `/api/v1/cart/items` | Add item to cart | ❌ Storefront-only |
| 81 | PUT | `/api/v1/cart/items/{cartItemId}` | Update cart item quantity | ❌ Storefront-only |
| 82 | DELETE | `/api/v1/cart/items/{cartItemId}` | Remove cart item | ❌ Storefront-only |
| 83 | DELETE | `/api/v1/cart/items` | Clear all items | ❌ Storefront-only |
| 84 | POST | `/api/v1/cart/coupon` | Apply coupon code | ❌ Storefront-only |

### 2.13 User Controller (`user-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 85 | GET | `/api/v1/account/profile` | Current user profile | ✅ `authApi.me` → AuthProvider |
| 86 | PUT | `/api/v1/account/profile` | Update profile | ❌ Storefront-only |
| 87 | DELETE | `/api/v1/account/profile` | Delete account | ❌ Storefront-only |
| 88 | GET | `/api/v1/account/addresses` | List addresses | ❌ Storefront-only |
| 89 | POST | `/api/v1/account/addresses` | Add address | ❌ Storefront-only |
| 90 | PUT | `/api/v1/account/addresses/{addressId}` | Update address | ❌ Storefront-only |
| 91 | DELETE | `/api/v1/account/addresses/{addressId}` | Delete address | ❌ Storefront-only |
| 92 | GET | `/api/v1/account/payment-methods` | List payment methods | ❌ Storefront-only |
| 93 | POST | `/api/v1/account/payment-methods` | Add payment method | ❌ Storefront-only |
| 94 | PUT | `/api/v1/account/payment-methods/{paymentMethodId}` | Update payment method | ❌ Storefront-only |
| 95 | DELETE | `/api/v1/account/payment-methods/{paymentMethodId}` | Delete payment method | ❌ Storefront-only |
| 96 | PUT | `/api/v1/account/company-profile` | Upsert B2B company profile | ❌ Storefront-only |
| 97 | GET | `/api/v1/account/orders` | Customer order history | ❌ Storefront-only |
| 98 | POST | `/api/v1/account/orders/{orderId}/quick-reorder` | Quick reorder | ❌ Storefront-only |
| 99 | GET | `/api/v1/account/approvals` | My B2B approvals | ❌ Storefront-only |

### 2.14 Recommendation Controller (`recommendation-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 100 | GET | `/api/v1/recommendations/trending` | Trending products | ❌ Storefront-only |
| 101 | GET | `/api/v1/recommendations/personalized` | Personalized picks | ❌ Storefront-only |
| 102 | GET | `/api/v1/recommendations/mock-ai` | AI-powered recommendations | ❌ Storefront-only |
| 103 | GET | `/api/v1/recommendations/deals` | Deal products | ❌ Storefront-only |
| 104 | GET | `/api/v1/recommendations/cross-sell` | Cross-sell suggestions | ❌ Storefront-only |
| 105 | GET | `/api/v1/recommendations/best-sellers` | Best sellers | ❌ Storefront-only |

### 2.15 Search Controller (`search-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 106 | GET | `/api/v1/search/predictive` | Predictive search suggestions | ❌ Storefront-only |
| 107 | GET | `/api/v1/search/trend-tags` | Trending search tags | ❌ Storefront-only |

---

## 3. Inventory API — All Endpoints

### 3.1 Inventory Auth Controller (`inventory-auth-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 1 | POST | `/api/inventory/v1/auth/login` | Inventory system login | ❌ Not used |
| 2 | POST | `/api/inventory/v1/auth/register` | Register inventory user | ❌ Not used |
| 3 | GET | `/api/inventory/v1/auth/me` | Current inventory user | ❌ Not used |

### 3.2 System Controller (`system-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 4 | GET | `/api/inventory/v1/system/status` | System health status | ✅ `systemApi.getInventorySystemStatus` → DashboardPage |

### 3.3 Product Controller — Inventory (`product-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 5 | GET | `/api/inventory/v1/products` | List products (paginated) | ✅ `inventoryProductsApi.listProducts` → 7 pages |
| 6 | POST | `/api/inventory/v1/products` | Create product | ✅ `inventoryProductsApi.createProduct` → CatalogPage |
| 7 | GET | `/api/inventory/v1/products/{productId}` | Get single product | ❌ Not used |
| 8 | PUT | `/api/inventory/v1/products/{productId}` | Update product | ✅ `inventoryProductsApi.updateProduct` → CatalogPage |
| 9 | DELETE | `/api/inventory/v1/products/{productId}` | Delete product | ✅ `inventoryProductsApi.deleteProduct` → CatalogPage |

### 3.4 Category Controller (`category-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 10 | GET | `/api/inventory/v1/categories` | List categories (paginated) | ✅ `inventoryCategoriesApi.listCategories` → DashboardPage, CatalogPage |
| 11 | POST | `/api/inventory/v1/categories` | Create category | ✅ `inventoryCategoriesApi.createCategory` → CatalogPage |
| 12 | GET | `/api/inventory/v1/categories/{categoryId}` | Get single category | ❌ Not used |
| 13 | PUT | `/api/inventory/v1/categories/{categoryId}` | Update category | ✅ `inventoryCategoriesApi.updateCategory` → CatalogPage |
| 14 | DELETE | `/api/inventory/v1/categories/{categoryId}` | Delete category | ✅ `inventoryCategoriesApi.deleteCategory` → CatalogPage |
| 15 | GET | `/api/inventory/v1/categories/tree` | Category tree | ✅ `inventoryCategoriesApi.getCategoryTree` → CatalogPage |

### 3.5 Warehouse Controller (`warehouse-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 16 | GET | `/api/inventory/v1/warehouses` | List warehouses | ✅ `inventoryLocationsApi.listWarehouses` → 6 pages |
| 17 | POST | `/api/inventory/v1/warehouses` | Create warehouse | ✅ `inventoryLocationsApi.createWarehouse` → LocationsPage |
| 18 | GET | `/api/inventory/v1/warehouses/{warehouseId}` | Get single warehouse | ❌ Not used |
| 19 | PUT | `/api/inventory/v1/warehouses/{warehouseId}` | Update warehouse | ✅ `inventoryLocationsApi.updateWarehouse` → LocationsPage |
| 20 | DELETE | `/api/inventory/v1/warehouses/{warehouseId}` | Delete warehouse | ✅ `inventoryLocationsApi.deleteWarehouse` → LocationsPage |
| 21 | GET | `/api/inventory/v1/warehouses/{warehouseId}/bins` | List bins in warehouse | ✅ `inventoryLocationsApi.listWarehouseBins` → LocationsPage, SerialsPage |
| 22 | POST | `/api/inventory/v1/warehouses/{warehouseId}/bins` | Create bin | ✅ `inventoryLocationsApi.createWarehouseBin` → LocationsPage |

### 3.6 Warehouse Bin Controller (`warehouse-bin-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 23 | GET | `/api/inventory/v1/bins` | List all bins (cross-warehouse) | ✅ `inventoryLocationsApi.listBins` → InventoryPage, MovementsPage, ReportsPage |
| 24 | GET | `/api/inventory/v1/bins/{binId}` | Get single bin | ❌ Not used |
| 25 | PUT | `/api/inventory/v1/bins/{binId}` | Update bin | ✅ `inventoryLocationsApi.updateWarehouseBin` → LocationsPage |
| 26 | DELETE | `/api/inventory/v1/bins/{binId}` | Delete bin | ✅ `inventoryLocationsApi.deleteWarehouseBin` → LocationsPage |

### 3.7 Stock Level Controller (`stock-level-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 27 | GET | `/api/inventory/v1/stock-levels` | List stock levels (filterable) | ✅ `inventoryApi.listStockLevels` → DashboardPage, InventoryPage |

### 3.8 Stock Movement Controller (`stock-movement-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 28 | GET | `/api/inventory/v1/movements` | List movements (paginated) | ✅ `movementsApi.listMovements` → DashboardPage, MovementsPage |
| 29 | GET | `/api/inventory/v1/movements/{movementId}` | Get single movement | ❌ Not used |
| 30 | POST | `/api/inventory/v1/movements/inbound` | Receive inbound stock | ✅ `movementsApi.receiveInbound` → MovementsPage |
| 31 | POST | `/api/inventory/v1/movements/outbound` | Ship outbound stock | ✅ `movementsApi.shipOutbound` → MovementsPage |
| 32 | POST | `/api/inventory/v1/movements/transfers` | Transfer between warehouses | ✅ `movementsApi.transferStock` → MovementsPage |
| 33 | POST | `/api/inventory/v1/movements/adjustments` | Stock adjustments | ✅ `movementsApi.adjustStock` → MovementsPage, InventoryPage |
| 34 | POST | `/api/inventory/v1/movements/returns` | Return stock | ✅ `movementsApi.returnStock` → MovementsPage |

### 3.9 Batch/Lot & Serial Controllers

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 35 | GET | `/api/inventory/v1/batches` | List batch lots | ✅ `reportsApi.listBatchLots` → BatchesPage |
| 36 | GET | `/api/inventory/v1/batches/{batchId}` | Get single batch | ✅ `reportsApi.getBatchLot` → BatchesPage |
| 37 | GET | `/api/inventory/v1/serials` | List serial numbers | ✅ `reportsApi.listSerialNumbers` → SerialsPage |
| 38 | GET | `/api/inventory/v1/serials/{serialId}` | Get single serial | ✅ `reportsApi.getSerialNumber` → SerialsPage |

### 3.10 Reporting Controller (`inventory-reporting-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 39 | GET | `/api/inventory/v1/reports/stock-valuation` | Stock valuation report | ✅ `reportsApi.getStockValuationReport` → DashboardPage, ReportsPage |
| 40 | GET | `/api/inventory/v1/reports/low-stock` | Low stock alerts | ✅ `reportsApi.getLowStockReport` → DashboardPage, ReportsPage |
| 41 | GET | `/api/inventory/v1/reports/turnover` | Inventory turnover report | ✅ `reportsApi.getTurnoverReport` → DashboardPage, ReportsPage |
| 42 | GET | `/api/inventory/v1/reports/movement-history` | Movement history report | ✅ `reportsApi.getMovementHistory` → ReportsPage |
| 43 | GET | `/api/inventory/v1/reports/export` | Export report as CSV | ✅ `reportsApi.exportReportCsv` → ReportsPage |

### 3.11 Barcode Controller (`inventory-barcode-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 44 | GET | `/api/inventory/v1/barcodes/products/{productId}` | Product barcode image | ✅ `reportsApi.getBarcodeAsset` → ReportsPage |
| 45 | GET | `/api/inventory/v1/barcodes/batches/{batchId}` | Batch barcode image | ✅ `reportsApi.getBarcodeAsset` → ReportsPage |
| 46 | GET | `/api/inventory/v1/barcodes/bins/{binId}` | Bin barcode image | ✅ `reportsApi.getBarcodeAsset` → ReportsPage |

### 3.12 Webhook Controller (`webhook-subscription-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 47 | GET | `/api/inventory/v1/webhooks` | List webhook subscriptions | ✅ `webhooksApi.listWebhookSubscriptions` → WebhooksPage |
| 48 | POST | `/api/inventory/v1/webhooks` | Create subscription | ✅ `webhooksApi.createWebhookSubscription` → WebhooksPage |
| 49 | GET | `/api/inventory/v1/webhooks/{subscriptionId}` | Get single subscription | ⚠️ Exported but unused |
| 50 | PUT | `/api/inventory/v1/webhooks/{subscriptionId}` | Update subscription | ✅ `webhooksApi.updateWebhookSubscription` → WebhooksPage |
| 51 | DELETE | `/api/inventory/v1/webhooks/{subscriptionId}` | Delete subscription | ✅ `webhooksApi.deleteWebhookSubscription` → WebhooksPage |

### 3.13 Audit Log Controller (`audit-log-controller`)

| # | Method | Path | Purpose | Used in Admin? |
|---|--------|------|---------|----------------|
| 52 | GET | `/api/inventory/v1/audit-logs` | List audit logs (filterable, paginated) | ✅ `auditLogsApi.listAuditLogs` → AuditLogsPage |

---

## 4. Admin Dashboard Usage Matrix

### Page → API Endpoint Mapping

| Page | Route | Endpoints Used |
|------|-------|----------------|
| **LoginPage** | `/login` | `POST /auth/login` |
| **DashboardPage** | `/admin` | `GET /inv/categories`, `GET /inv/warehouses`, `GET /inv/movements`, `GET /inv/products`, `GET /inv/reports/low-stock`, `GET /inv/reports/stock-valuation`, `GET /inv/reports/turnover`, `GET /inv/stock-levels`, `GET /inv/system/status` |
| **AnalyticsPage** | `/admin/analytics` | `GET /orders`, `GET /categories/analytics` |
| **CommerceCatalogPage** | `/admin/commerce/catalog` | `GET /categories/tree`, `POST /categories`, `PUT /categories/{id}`, `GET /products`, `GET /products/{id}`, `POST /products`, `PUT /products/{id}`, `PATCH /products/{id}`, `DELETE /products/{id}`, `POST /products/{id}/variants`, `PUT /variants/{id}`, `POST /products/{id}/media`, `PUT /products/{id}/inventory`, `GET /stores` |
| **OrdersPage** | `/admin/orders` | `GET /orders`, `GET /orders/{id}/timeline`, `PATCH /orders/{id}/status` |
| **ReturnsPage** | `/admin/returns` | `GET /orders`, `PATCH /orders/{id}/status` |
| **StoresPage** | `/admin/stores` | `GET /stores`, `POST /stores`, `PUT /stores/{id}`, `DELETE /stores/{id}` |
| **PricingPage** | `/admin/pricing` | `GET /price-lists`, `POST /price-lists`, `POST /prices`, `GET /prices/variants/{id}`, `GET /promotions/active`, `POST /promotions` |
| **UsersPage** | `/admin/users` | `GET /admin/users`, `PATCH /admin/users/{id}` |
| **NotificationsPage** | `/admin/notifications` | All 6 notification endpoints |
| **ControlCenterPage** | `/admin/tools/control-center` | `GET /admin/b2b/approvals`, `PATCH /admin/b2b/approvals/{id}`, + dynamic workbench |
| **CatalogPage** | `/admin/warehouse/catalog` | 5 inventory category + 4 inventory product endpoints |
| **LocationsPage** | `/admin/warehouse/locations` | 9 warehouse + bin CRUD endpoints |
| **InventoryPage** | `/admin/warehouse/stock` | `GET /inv/stock-levels`, `GET /inv/warehouses`, `GET /inv/bins`, `GET /inv/products`, `POST /inv/movements/adjustments` |
| **MovementsPage** | `/admin/warehouse/movements` | `GET /inv/warehouses`, `GET /inv/bins`, `GET /inv/products`, all 6 movement endpoints |
| **BatchesPage** | `/admin/warehouse/batches` | `GET /inv/products`, `GET /inv/batches`, `GET /inv/batches/{id}` |
| **SerialsPage** | `/admin/warehouse/serials` | `GET /inv/products`, `GET /inv/warehouses`, `GET /inv/warehouses/{id}/bins`, `GET /inv/serials`, `GET /inv/serials/{id}` |
| **ReportsPage** | `/admin/warehouse/reports` | `GET /inv/warehouses`, `GET /inv/bins`, `GET /inv/products`, all 5 report endpoints, `GET /inv/reports/export`, 3 barcode endpoints |
| **WebhooksPage** | `/admin/warehouse/webhooks` | 4 of 5 webhook endpoints |
| **AuditLogsPage** | `/admin/warehouse/audit-logs` | `GET /inv/audit-logs` |

---

## 5. Unused Backend Endpoints (Gaps)

### 5.1 High-Value — Should Be Added to Admin Dashboard

These endpoints represent **existing backend functionality** that would significantly enhance the admin dashboard:

| Priority | Endpoint | Why It Matters |
|----------|----------|---------------|
| **P0** | `GET /admin/dashboard/summary` | Returns revenue, orders, users, stores KPIs — the DashboardPage currently uses only inventory endpoints and misses commerce KPIs. **Wire this endpoint to the dashboard.** |
| **P0** | `GET /orders/{orderId}` | Enables order detail view. Currently admin can only see the list and timeline, not full order details. |
| **P1** | `GET /products/{productId}/variants` | List a product's variants. Essential for variant management in CommerceCatalogPage. |
| **P1** | `GET /products/{productId}/reviews` | View customer reviews. Add a reviews panel to CommerceCatalogPage. |
| **P1** | `POST /auth/refresh` | Token refresh prevents session expiry. Currently users get logged out on token expiry via 401 interceptor. **Critical for UX.** |
| **P1** | `POST /auth/password-reset/request` + `confirm` | Admin should be able to reset their password. Add to LoginPage. |
| **P2** | `PUT /categories/{categoryId}/translations/{locale}` + `GET …/translations` | Category i18n support. The backend supports it but the UI doesn't expose it. |
| **P2** | `POST /categories/channel-mappings` + `GET …/channel-mappings` | Multi-channel catalog mapping (e.g. Amazon, Shopee categories). |
| **P2** | `GET/POST /categories/change-requests` + `approve/reject` | Category governance workflow — submit/approve/reject changes. |
| **P2** | `POST /categories/ai/suggest` | AI-powered category suggestions for products. Could enhance CommerceCatalogPage. |
| **P2** | `POST /attributes` + `POST /attribute-sets` | Structured product attributes. Currently attributes are freeform JSON. |
| **P3** | `GET /inv/products/{productId}` | Single product detail view for inventory context. |
| **P3** | `GET /inv/warehouses/{warehouseId}` | Single warehouse detail view. |
| **P3** | `GET /inv/categories/{categoryId}` | Single category detail view. |
| **P3** | `GET /inv/bins/{binId}` | Single bin detail view. |
| **P3** | `GET /inv/movements/{movementId}` | Movement detail view. |
| **P3** | `GET /inv/webhooks/{subscriptionId}` | Already exported in webhooksApi but not imported. Wire it up. |

### 5.2 Storefront-Only — Correctly Excluded from Admin

These are customer-facing endpoints and do NOT need admin dashboard integration:

- Cart endpoints (`/cart/*`) — 5 endpoints
- Checkout endpoints (`/checkout/*`) — 5 endpoints
- Account self-service (`/account/addresses`, `/account/payment-methods`, `/account/orders`, `/account/approvals`, `/account/company-profile`, `/account/profile` PUT/DELETE) — 12 endpoints
- Store preferences (`/stores/preferred/{storeId}`, `/stores/nearest`) — 2 endpoints
- Recommendations (`/recommendations/*`) — 6 endpoints
- Search (`/search/*`) — 2 endpoints
- Product storefront features (`/products/{id}/related`, `/products/{id}/frequently-bought-together`, `/products/trend-tags`) — 3 endpoints
- Inventory reservation flow (`/inventory/reserve`, `/inventory/release`, `/inventory/confirm`, `/inventory/check`, `/inventory/adjust`) — 5 endpoints
- Platform warehouse endpoints (`/inventory/warehouses`) — 2 endpoints (duplicated in inventory API)
- Inventory auth (`/inv/auth/*`) — 3 endpoints (uses platform auth instead)

---

## 6. Orphan Frontend Exports

Functions exported in API files but **never imported by any page component**:

| Function | File | Recommendation |
|----------|------|---------------|
| `getDashboardSummary` | `dashboardApi.js` | **Use it!** Wire to DashboardPage for commerce KPIs (revenue, orderCount, usersCount, storesCount, topProducts). Currently the dashboard only shows inventory metrics. |
| `listCommerceInventories` | `commerceProductsApi.js` | Wire to CommerceCatalogPage — show per-store inventory in the product detail/edit view. |
| `getWebhookSubscription` | `webhooksApi.js` | Wire to WebhooksPage — show detail panel when clicking a webhook subscription. |
| `registerUser` | `authApi.js` | Consider adding admin user creation flow using this, or remove the export if admins can only be created server-side. |

---

## 7. Improvement Recommendations

### 7.1 UI Improvements

| Area | Current State | Recommendation |
|------|--------------|----------------|
| **Dashboard** | Shows only inventory metrics (stock levels, movements, warehouses, low stock, valuation, turnover) | Add a **Commerce KPIs** section using `GET /admin/dashboard/summary` — show revenue, orderCount, usersCount, storesCount, topProducts. Create a split dashboard with Commerce + Inventory tabs. |
| **Order Detail** | Only list view + timeline sidebar | Add **Order Detail Modal/Panel** using `GET /orders/{orderId}` — show full order with line items, shipping address, payment info, status badges. |
| **Product Reviews** | Not visible at all | Add **Reviews Tab** in CommerceCatalogPage product detail using `GET /products/{id}/reviews`. |
| **Variant Management** | Can add variants but cannot list/view existing variants separately | Add **Variants Table** using `GET /products/{id}/variants` in CommerceCatalogPage. |
| **Category Translations** | Not supported | Add **Translations Panel** in CommerceCatalogPage category sidebar using translation endpoints. Critical for multi-locale commerce. |
| **Category Change Requests** | Not supported | Add **Change Request Queue** page or section — workflow for category governance. |
| **AI Category Suggestions** | Not supported | Add **"Suggest Category" button** in product form using `POST /categories/ai/suggest`. |
| **Attribute Management** | Attributes stored as freeform JSON | Add dedicated **Attributes Page** using `POST /attributes` and `POST /attribute-sets` for structured attribute management. |
| **Password Reset** | Not available | Add **"Forgot Password" link** on LoginPage using password-reset endpoints. |
| **Token Refresh** | Not implemented — 401 forces re-login | Implement **silent token refresh** in `httpClient.js` using `POST /auth/refresh` before token expiry. |
| **Notification Badge** | Not shown in navigation | Add **unread count badge** in header/nav using `GET /notifications/me/unread-count` (endpoint exists, wire to layout). |
| **Movement Detail** | Only list view | Add detail drill-down using `GET /inv/movements/{movementId}`. |
| **Webhook Detail** | Only list view | Add detail panel using `GET /inv/webhooks/{subscriptionId}`. |

### 7.2 Functionality Improvements

| Area | Recommendation |
|------|---------------|
| **Bulk Operations** | Backend supports pagination — add select-all, bulk delete, bulk status update for products, orders. |
| **Real-time Updates** | Consider WebSocket/SSE for stock level changes, order status updates, notification delivery. |
| **Export/Import** | The backend supports CSV export for inventory (`GET /inv/reports/export`). Add export buttons for orders, products, and users too. |
| **Search** | The product list supports `query`, `category`, `brand`, `minPrice`, `maxPrice`, `minRating`, `storeId`, `flashSale`, `trending`, `attributeKey/Value` filters — many aren't exposed in the CommerceCatalogPage UI. Add advanced filter panel. |
| **Sorting** | All paginated endpoints support `sortBy` and `direction` — wire click-to-sort on table column headers. |
| **Channel Mapping** | Add channel management for marketplace integrations using the channel-mapping endpoints. |

### 7.3 Security Improvements

| Area | Current State | Recommendation |
|------|--------------|----------------|
| **Token Refresh** | No refresh flow; 401 → redirect to login | Implement refresh token rotation in axios interceptor. Store refresh token in httpOnly cookie if possible, not localStorage. |
| **Password Reset** | Not implemented in admin | Add password-reset flow for admin users. |
| **RBAC in UI** | Backend has roles (ADMIN, CUSTOMER, B2B) | Add route guards and feature flags based on user roles. Hide admin-only features from B2B users. |
| **CSRF Protection** | Not verified | Verify Spring Security CSRF configuration for state-changing endpoints (POST/PUT/PATCH/DELETE). |
| **Rate Limiting** | Not visible in spec | Add rate limiting for auth endpoints (login, register, password-reset) to prevent brute force. |
| **Input Validation** | Backend validates (see `required`, `minLength`, `maxLength`, `minimum`, `maximum` in schemas) | Mirror key validations client-side for better UX. Currently forms may allow submission of data the backend will reject. |
| **Audit Logging** | Only inventory API has audit logs | Extend audit logging to platform API (product changes, order status updates, user role changes). |
| **Session Timeout** | Token expiry = session end | Add idle timeout warning + automatic logout after inactivity. |

---

## 8. Security Audit

### Authentication & Authorization
- **Auth scheme**: Bearer JWT (`bearerAuth`)
- **Token storage**: Verify tokens are stored securely (prefer `httpOnly` cookies over `localStorage`)
- **Missing**: Token refresh flow (backend supports `POST /auth/refresh` but frontend doesn't use it)
- **Missing**: Password reset flow (backend supports it but frontend doesn't expose it)
- **Risk**: No CSRF token visible in the spec — verify Spring Security configuration

### Input Validation Summary (from OpenAPI schemas)

| DTO | Key Validations |
|-----|----------------|
| `LoginRequest` | `email` required, `password` required |
| `RegisterRequest` | `email` required, `fullName` required, `password` min=8, max=72 |
| `PasswordResetConfirmRequest` | `token` required, `newPassword` min=8, max=72 |
| `ProductRequest` | Only `name` required — consider adding more required fields |
| `StoreRequest` | 14 required fields — well validated |
| `WarehouseRequest` (inventory) | `warehouseCode`, `name`, `warehouseType` required; maxLength constraints |
| `WarehouseBinRequest` | `binCode`, `binType` required; maxLength constraints |
| `WebhookSubscriptionRequest` | `eventCode` max=128, `endpointUrl` max=1000, `timeoutMs` 1000-60000, `retryCount` 0-10 |
| `StockMovementLineRequest` | `productId` required |
| `AdminUserUpdateRequest` | Only `roles` and `enabled` — no required fields |

### Recommendations
1. **ProductRequest** has only `name` required — consider requiring `category`, `brand`, or `price` as well
2. **AdminUserUpdateRequest** has no required fields — consider requiring at least one field
3. Add `@Validated` group checks if not already present for different create vs. update schemas
4. Verify all DELETE endpoints properly check ownership/permissions server-side

---

## 9. Endpoint Optimization Guidance

### 9.1 Pagination Consistency
All paginated endpoints consistently use `page`, `size`, `sortBy`, `direction` parameters. This is well-designed. Ensure the frontend always passes these to avoid unbounded queries.

### 9.2 N+1 Query Risks
- **DashboardPage** makes **9 parallel API calls** on load. Consider creating a dedicated `GET /admin/dashboard/overview` endpoint that aggregates all dashboard data in one call.
- **CommerceCatalogPage** loads categories + products + stores on mount — consider a combined endpoint or lazy loading.

### 9.3 Caching Opportunities
| Endpoint | Cache Strategy |
|----------|---------------|
| `GET /categories/tree` | Cache for 5-10 minutes — tree changes rarely |
| `GET /stores` | Cache for 5 minutes — store data is relatively static |
| `GET /price-lists` | Cache for 5 minutes |
| `GET /promotions/active` | Cache for 1-2 minutes |
| `GET /system/status` | Cache for 30 seconds |
| `GET /admin/dashboard/summary` | Cache for 1-2 minutes |
| Barcode endpoints | Cache indefinitely (content-addressable) |

### 9.4 Payload Optimization
- Product list returns full `ProductDto` with nested `variants`, `media`, and `storeInventory`. For table views, add a `view=summary` parameter (already supported: `view` param exists in the spec with default `grid`) — ensure the backend returns minimal fields for `view=table` or `view=list`.
- Order list returns full order items — for list views, consider a summary DTO.

### 9.5 API Design Improvements
| Issue | Recommendation |
|-------|---------------|
| Duplicate warehouse endpoints | Platform (`/api/v1/inventory/warehouses`) and Inventory (`/api/inventory/v1/warehouses`) both exist. Deprecate the platform one. |
| Duplicate variant stock endpoints | `GET /inventory/{variantId}` and `GET /inventory/variants/{variantId}` do the same thing. Deprecate one. |
| Missing DELETE for categories | Platform categories `DELETE /categories/{id}` is missing from the spec. Add it or use change-request workflow. |
| Missing pagination for some lists | `GET /notifications/me`, `GET /price-lists`, `GET /promotions/active` return arrays not pages. Add pagination for scalability. |
| Inconsistent ID types | Platform uses UUID format, Inventory uses plain strings. Standardize to UUID across both APIs. |
| No `GET` single order in admin | `GET /orders/{orderId}` exists but admin dashboard doesn't use it. |

### 9.6 Recommended New Backend Endpoints
| Endpoint | Purpose |
|----------|---------|
| `GET /admin/dashboard/overview` | Aggregated commerce + inventory KPIs in one call |
| `GET /admin/orders/{orderId}` | Admin-specific order detail with additional info |
| `DELETE /categories/{categoryId}` | Direct category deletion (currently missing) |
| `GET /admin/analytics/overview` | Combined analytics (orders + categories + revenue trends) |
| `PATCH /promotions/{promotionId}` | Update/deactivate existing promotions |
| `DELETE /promotions/{promotionId}` | Delete promotions |
| `PATCH /price-lists/{priceListId}` | Update price lists |
| `DELETE /price-lists/{priceListId}` | Delete price lists |
| `GET /admin/export/orders` | Export orders as CSV |
| `GET /admin/export/products` | Export products as CSV |
| `GET /admin/audit-logs` | Platform-side audit logs (not just inventory) |

---

## Summary Scorecard

| Category | Score | Notes |
|----------|-------|-------|
| **Inventory API coverage** | 🟢 **95%** | Nearly all endpoints used. Only missing single-entity GETs and inventory auth. |
| **Commerce API coverage** | 🟡 **60%** | Core CRUD well-covered. Missing: translations, channel mappings, change requests, AI suggestions, attributes, token refresh, password reset. |
| **Storefront API exclusion** | 🟢 **Correct** | Cart, checkout, account, recommendation, and search endpoints appropriately excluded. |
| **Security posture** | 🟡 **Moderate** | JWT auth in place, but missing token refresh, password reset, CSRF verification, rate limiting, and RBAC in UI. |
| **API consistency** | 🟡 **Good** | Consistent pagination and response envelopes. Some duplicate endpoints and inconsistent ID types between APIs. |
| **UI completeness** | 🟡 **Good** | All major admin flows exist. Missing: order detail, reviews, translations, category governance, advanced product search. |

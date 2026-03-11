# Frontend API Audit

This audit captures every API wrapper in the frontend apps. Endpoints are listed as fully qualified (including `/api/v1` or `/api/inventory/v1`). The "Current Endpoint" reflects the code after the latest refactor.

## Admin Dashboard (`frontend/admin-dashboard`)

| App | File | Function | Current Endpoint | Intended Endpoint | Keep/Change | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/adminApi.js | listApprovalQueue | GET `/api/v1/admin/b2b/approvals` | GET `/api/v1/admin/b2b/approvals` | Keep | Commerce admin approvals. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/adminApi.js | updateApproval | PATCH `/api/v1/admin/b2b/approvals/{approvalId}` | PATCH `/api/v1/admin/b2b/approvals/{approvalId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/adminApi.js | listAdminUsers | GET `/api/v1/admin/users` | GET `/api/v1/admin/users` | Keep | Pageable params. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/adminApi.js | updateAdminUser | PATCH `/api/v1/admin/users/{userId}` | PATCH `/api/v1/admin/users/{userId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/auditLogsApi.js | listAuditLogs | GET `/api/inventory/v1/audit-logs` | GET `/api/inventory/v1/audit-logs` | Keep | Inventory domain. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/authApi.js | loginPassword | POST `/api/v1/auth/login` | POST `/api/v1/auth/login` | Keep | Payload normalized. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/authApi.js | registerUser | POST `/api/v1/auth/register` | POST `/api/v1/auth/register` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/authApi.js | me | GET `/api/v1/account/profile` | GET `/api/v1/account/profile` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/commerceCategoriesApi.js | getCommerceCategoryTree | GET `/api/v1/categories/tree` | GET `/api/v1/categories/tree` | Keep | Optional locale param. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/commerceCategoriesApi.js | createCommerceCategory | POST `/api/v1/categories` | POST `/api/v1/categories` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/commerceCategoriesApi.js | updateCommerceCategory | PUT `/api/v1/categories/{categoryId}` | PUT `/api/v1/categories/{categoryId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/commerceProductsApi.js | listCommerceProducts | GET `/api/v1/products` | GET `/api/v1/products` | Keep | Pageable params. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/commerceProductsApi.js | getCommerceProduct | GET `/api/v1/products/{productId}` | GET `/api/v1/products/{productId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/commerceProductsApi.js | createCommerceProduct | POST `/api/v1/products` | POST `/api/v1/products` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/commerceProductsApi.js | updateCommerceProduct | PUT `/api/v1/products/{productId}` | PUT `/api/v1/products/{productId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/commerceProductsApi.js | patchCommerceProduct | PATCH `/api/v1/products/{productId}` | PATCH `/api/v1/products/{productId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/commerceProductsApi.js | deleteCommerceProduct | DELETE `/api/v1/products/{productId}` | DELETE `/api/v1/products/{productId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/commerceProductsApi.js | addCommerceVariant | POST `/api/v1/products/{productId}/variants` | POST `/api/v1/products/{productId}/variants` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/commerceProductsApi.js | updateCommerceVariant | PUT `/api/v1/variants/{variantId}` | PUT `/api/v1/variants/{variantId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/commerceProductsApi.js | addCommerceMedia | POST `/api/v1/products/{productId}/media` | POST `/api/v1/products/{productId}/media` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/commerceProductsApi.js | upsertCommerceStoreInventory | PUT `/api/v1/products/{productId}/inventory` | PUT `/api/v1/products/{productId}/inventory` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/commerceProductsApi.js | listCommerceInventories | GET `/api/v1/products/{productId}/inventory` | GET `/api/v1/products/{productId}/inventory` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/dashboardApi.js | getDashboardSummary | GET `/api/v1/admin/dashboard/summary` | GET `/api/v1/admin/dashboard/summary` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryApi.js | listStockLevels | GET `/api/inventory/v1/stock-levels` | GET `/api/inventory/v1/stock-levels` | Keep | Inventory domain. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryCategoriesApi.js | listCategories | GET `/api/inventory/v1/categories` | GET `/api/inventory/v1/categories` | Keep | Pageable params. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryCategoriesApi.js | getCategoryTree | GET `/api/inventory/v1/categories/tree` | GET `/api/inventory/v1/categories/tree` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryCategoriesApi.js | createCategory | POST `/api/inventory/v1/categories` | POST `/api/inventory/v1/categories` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryCategoriesApi.js | updateCategory | PUT `/api/inventory/v1/categories/{categoryId}` | PUT `/api/inventory/v1/categories/{categoryId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryCategoriesApi.js | deleteCategory | DELETE `/api/inventory/v1/categories/{categoryId}` | DELETE `/api/inventory/v1/categories/{categoryId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryLocationsApi.js | listWarehouses | GET `/api/inventory/v1/warehouses` | GET `/api/inventory/v1/warehouses` | Keep | Pageable params. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryLocationsApi.js | createWarehouse | POST `/api/inventory/v1/warehouses` | POST `/api/inventory/v1/warehouses` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryLocationsApi.js | updateWarehouse | PUT `/api/inventory/v1/warehouses/{warehouseId}` | PUT `/api/inventory/v1/warehouses/{warehouseId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryLocationsApi.js | deleteWarehouse | DELETE `/api/inventory/v1/warehouses/{warehouseId}` | DELETE `/api/inventory/v1/warehouses/{warehouseId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryLocationsApi.js | listWarehouseBins | GET `/api/inventory/v1/warehouses/{warehouseId}/bins` | GET `/api/inventory/v1/warehouses/{warehouseId}/bins` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryLocationsApi.js | listBins | GET `/api/inventory/v1/bins` | GET `/api/inventory/v1/bins` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryLocationsApi.js | createWarehouseBin | POST `/api/inventory/v1/warehouses/{warehouseId}/bins` | POST `/api/inventory/v1/warehouses/{warehouseId}/bins` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryLocationsApi.js | updateWarehouseBin | PUT `/api/inventory/v1/bins/{binId}` | PUT `/api/inventory/v1/bins/{binId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryLocationsApi.js | deleteWarehouseBin | DELETE `/api/inventory/v1/bins/{binId}` | DELETE `/api/inventory/v1/bins/{binId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryProductsApi.js | listProducts | GET `/api/inventory/v1/products` | GET `/api/inventory/v1/products` | Keep | Pageable params. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryProductsApi.js | createProduct | POST `/api/inventory/v1/products` | POST `/api/inventory/v1/products` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryProductsApi.js | updateProduct | PUT `/api/inventory/v1/products/{productId}` | PUT `/api/inventory/v1/products/{productId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/inventoryProductsApi.js | deleteProduct | DELETE `/api/inventory/v1/products/{productId}` | DELETE `/api/inventory/v1/products/{productId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/movementsApi.js | listMovements | GET `/api/inventory/v1/movements` | GET `/api/inventory/v1/movements` | Keep | Pageable params. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/movementsApi.js | receiveInbound | POST `/api/inventory/v1/movements/inbound` | POST `/api/inventory/v1/movements/inbound` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/movementsApi.js | shipOutbound | POST `/api/inventory/v1/movements/outbound` | POST `/api/inventory/v1/movements/outbound` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/movementsApi.js | transferStock | POST `/api/inventory/v1/movements/transfers` | POST `/api/inventory/v1/movements/transfers` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/movementsApi.js | adjustStock | POST `/api/inventory/v1/movements/adjustments` | POST `/api/inventory/v1/movements/adjustments` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/movementsApi.js | returnStock | POST `/api/inventory/v1/movements/returns` | POST `/api/inventory/v1/movements/returns` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/notificationsApi.js | listMyNotifications | GET `/api/v1/notifications/me` | GET `/api/v1/notifications/me` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/notificationsApi.js | getUnreadCount | GET `/api/v1/notifications/me/unread-count` | GET `/api/v1/notifications/me/unread-count` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/notificationsApi.js | markNotificationRead | PATCH `/api/v1/notifications/{notificationId}/read` | PATCH `/api/v1/notifications/{notificationId}/read` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/notificationsApi.js | markAllNotificationsRead | PATCH `/api/v1/notifications/me/read-all` | PATCH `/api/v1/notifications/me/read-all` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/notificationsApi.js | pushNotificationToUser | POST `/api/v1/notifications/user/{userId}` | POST `/api/v1/notifications/user/{userId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/notificationsApi.js | broadcastNotification | POST `/api/v1/notifications/broadcast` | POST `/api/v1/notifications/broadcast` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/ordersApi.js | listOrders | GET `/api/v1/orders` | GET `/api/v1/orders` | Keep | Pageable params. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/ordersApi.js | getOrderTimeline | GET `/api/v1/orders/{orderId}/timeline` | GET `/api/v1/orders/{orderId}/timeline` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/ordersApi.js | updateOrderStatus | PATCH `/api/v1/orders/{orderId}/status` | PATCH `/api/v1/orders/{orderId}/status` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/pricingApi.js | listPriceLists | GET `/api/v1/price-lists` | GET `/api/v1/price-lists` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/pricingApi.js | createPriceList | POST `/api/v1/price-lists` | POST `/api/v1/price-lists` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/pricingApi.js | upsertPrice | POST `/api/v1/prices` | POST `/api/v1/prices` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/pricingApi.js | quoteVariantPrice | GET `/api/v1/prices/variants/{variantId}` | GET `/api/v1/prices/variants/{variantId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/pricingApi.js | listActivePromotions | GET `/api/v1/promotions/active` | GET `/api/v1/promotions/active` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/pricingApi.js | createPromotion | POST `/api/v1/promotions` | POST `/api/v1/promotions` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/reportsApi.js | getStockValuationReport | GET `/api/inventory/v1/reports/stock-valuation` | GET `/api/inventory/v1/reports/stock-valuation` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/reportsApi.js | getLowStockReport | GET `/api/inventory/v1/reports/low-stock` | GET `/api/inventory/v1/reports/low-stock` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/reportsApi.js | getTurnoverReport | GET `/api/inventory/v1/reports/turnover` | GET `/api/inventory/v1/reports/turnover` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/reportsApi.js | getMovementHistory | GET `/api/inventory/v1/reports/movement-history` | GET `/api/inventory/v1/reports/movement-history` | Keep | Pageable params. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/reportsApi.js | exportReportCsv | GET `/api/inventory/v1/reports/export` | GET `/api/inventory/v1/reports/export` | Keep | Returns CSV blob. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/reportsApi.js | listBatchLots | GET `/api/inventory/v1/batches` | GET `/api/inventory/v1/batches` | Keep | Pageable params. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/reportsApi.js | getBatchLot | GET `/api/inventory/v1/batches/{batchId}` | GET `/api/inventory/v1/batches/{batchId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/reportsApi.js | listSerialNumbers | GET `/api/inventory/v1/serials` | GET `/api/inventory/v1/serials` | Keep | Pageable params. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/reportsApi.js | getSerialNumber | GET `/api/inventory/v1/serials/{serialId}` | GET `/api/inventory/v1/serials/{serialId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/reportsApi.js | getBarcodeAsset | GET `/api/inventory/v1/barcodes/{resourceType}/{resourceId}` | GET `/api/inventory/v1/barcodes/{resourceType}/{resourceId}` | Change | Now uses inventory base URL + relative `/barcodes/...`. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/storesApi.js | listStores | GET `/api/v1/stores` | GET `/api/v1/stores` | Keep | Pageable params. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/storesApi.js | createStore | POST `/api/v1/stores` | POST `/api/v1/stores` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/storesApi.js | updateStore | PUT `/api/v1/stores/{storeId}` | PUT `/api/v1/stores/{storeId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/storesApi.js | getStoreLocation | GET `/api/v1/admin/stores/{storeId}/location` | GET `/api/v1/admin/stores/{storeId}/location` | Keep | Location-specific admin read model. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/storesApi.js | updateStoreLocation | PUT `/api/v1/admin/stores/{storeId}/location` | PUT `/api/v1/admin/stores/{storeId}/location` | Keep | Store coverage and service radius workflow. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/storesApi.js | deleteStore | DELETE `/api/v1/stores/{storeId}` | DELETE `/api/v1/stores/{storeId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/serviceAreasApi.js | listServiceAreas | GET `/api/v1/admin/service-areas` | GET `/api/v1/admin/service-areas` | Keep | Pageable params. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/serviceAreasApi.js | getServiceArea | GET `/api/v1/admin/service-areas/{serviceAreaId}` | GET `/api/v1/admin/service-areas/{serviceAreaId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/serviceAreasApi.js | createServiceArea | POST `/api/v1/admin/service-areas` | POST `/api/v1/admin/service-areas` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/serviceAreasApi.js | updateServiceArea | PUT `/api/v1/admin/service-areas/{serviceAreaId}` | PUT `/api/v1/admin/service-areas/{serviceAreaId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/serviceAreasApi.js | deleteServiceArea | DELETE `/api/v1/admin/service-areas/{serviceAreaId}` | DELETE `/api/v1/admin/service-areas/{serviceAreaId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/serviceAreasApi.js | activateServiceArea | POST `/api/v1/admin/service-areas/{serviceAreaId}/activate` | POST `/api/v1/admin/service-areas/{serviceAreaId}/activate` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/serviceAreasApi.js | deactivateServiceArea | POST `/api/v1/admin/service-areas/{serviceAreaId}/deactivate` | POST `/api/v1/admin/service-areas/{serviceAreaId}/deactivate` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/serviceAreasApi.js | validateServiceAreaRules | POST `/api/v1/admin/service-areas/validate` | POST `/api/v1/admin/service-areas/validate` | Keep | Admin coordinate sandbox. |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/systemApi.js | getInventorySystemStatus | GET `/api/inventory/v1/system/status` | GET `/api/inventory/v1/system/status` | Keep | Returns raw status payload (not wrapped). |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/webhooksApi.js | listSubscriptions | GET `/api/inventory/v1/webhooks` | GET `/api/inventory/v1/webhooks` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/webhooksApi.js | getSubscription | GET `/api/inventory/v1/webhooks/{subscriptionId}` | GET `/api/inventory/v1/webhooks/{subscriptionId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/webhooksApi.js | createSubscription | POST `/api/inventory/v1/webhooks` | POST `/api/inventory/v1/webhooks` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/webhooksApi.js | updateSubscription | PUT `/api/inventory/v1/webhooks/{subscriptionId}` | PUT `/api/inventory/v1/webhooks/{subscriptionId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/endpoints/webhooksApi.js | deleteSubscription | DELETE `/api/inventory/v1/webhooks/{subscriptionId}` | DELETE `/api/inventory/v1/webhooks/{subscriptionId}` | Keep | - |
| admin-dashboard | frontend/admin-dashboard/src/shared/api/rawRequest.js | executeRawApiRequest | User-supplied path | User-supplied path | Keep | Control Center uses `apiHost` + manual path; clamps `size` params to max 100. |

## Storefront (`frontend/storefront-noura`)

| App | File | Function | Current Endpoint | Intended Endpoint | Keep/Change | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| storefront | frontend/storefront-noura/lib/api.js | getCategories | GET `/api/v1/categories/tree` | GET `/api/v1/categories/tree` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | getProducts | GET `/api/v1/products` | GET `/api/v1/products` | Keep | Uses `query`, `categoryId`, `page`, `size`, `sortBy`, `direction`. |
| storefront | frontend/storefront-noura/lib/api.js | getProduct | GET `/api/v1/products/{productId}` | GET `/api/v1/products/{productId}` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | getAvailability | GET `/api/v1/products/{productId}/inventory` | GET `/api/v1/products/{productId}/inventory` | Keep | Commerce inventory view. |
| storefront | frontend/storefront-noura/lib/api.js | registerCustomer | POST `/api/v1/auth/register` | POST `/api/v1/auth/register` | Keep | Payload `{ fullName, email, password }`. |
| storefront | frontend/storefront-noura/lib/api.js | loginCustomer | POST `/api/v1/auth/login` | POST `/api/v1/auth/login` | Keep | Payload `{ email, password }`. |
| storefront | frontend/storefront-noura/lib/api.js | getCustomerMe | GET `/api/v1/account/profile` | GET `/api/v1/account/profile` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | getCustomerAddresses | GET `/api/v1/account/addresses` | GET `/api/v1/account/addresses` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | getCustomerAddress | GET `/api/v1/account/addresses/{addressId}` | GET `/api/v1/account/addresses/{addressId}` | Keep | Address detail endpoint for location-aware edits. |
| storefront | frontend/storefront-noura/lib/api.js | addCustomerAddress | POST `/api/v1/account/addresses` | POST `/api/v1/account/addresses` | Keep | Payload now includes phone, line2, district, coordinates, accuracy, placeId, formattedAddress, deliveryInstructions. |
| storefront | frontend/storefront-noura/lib/api.js | updateCustomerAddress | PUT `/api/v1/account/addresses/{addressId}` | PUT `/api/v1/account/addresses/{addressId}` | Keep | Same payload shape as create. |
| storefront | frontend/storefront-noura/lib/api.js | deleteCustomerAddress | DELETE `/api/v1/account/addresses/{addressId}` | DELETE `/api/v1/account/addresses/{addressId}` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | setDefaultCustomerAddress | POST `/api/v1/account/addresses/{addressId}/set-default` | POST `/api/v1/account/addresses/{addressId}/set-default` | Keep | Default-address workflow. |
| storefront | frontend/storefront-noura/lib/api.js | resolveLocation | POST `/api/v1/location/resolve` | POST `/api/v1/location/resolve` | Keep | Authenticated resolve + eligibility workflow. |
| storefront | frontend/storefront-noura/lib/api.js | reverseGeocode | POST `/api/v1/location/reverse-geocode` | POST `/api/v1/location/reverse-geocode` | Keep | Public geocoder wrapper. |
| storefront | frontend/storefront-noura/lib/api.js | forwardGeocode | POST `/api/v1/location/forward-geocode` | POST `/api/v1/location/forward-geocode` | Keep | Public place-search wrapper. |
| storefront | frontend/storefront-noura/lib/api.js | validateServiceArea | POST `/api/v1/location/validate-service-area` | POST `/api/v1/location/validate-service-area` | Keep | Delivery eligibility check. |
| storefront | frontend/storefront-noura/lib/api.js | getNearbyStores | GET `/api/v1/location/nearby-stores` | GET `/api/v1/location/nearby-stores` | Keep | Nearby service-point lookup. |
| storefront | frontend/storefront-noura/lib/api.js | getCart | GET `/api/v1/cart` | GET `/api/v1/cart` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | addCartItem | POST `/api/v1/cart/items` | POST `/api/v1/cart/items` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | updateCartItem | PUT `/api/v1/cart/items/{itemId}` | PUT `/api/v1/cart/items/{itemId}` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | removeCartItem | DELETE `/api/v1/cart/items/{itemId}` | DELETE `/api/v1/cart/items/{itemId}` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | clearCart | DELETE `/api/v1/cart/items` | DELETE `/api/v1/cart/items` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | checkoutCart | POST `/api/v1/checkout` | POST `/api/v1/checkout` | Keep | Now sends `addressId` and backend-derived shipping snapshot for delivery checkout. |
| storefront | frontend/storefront-noura/lib/api.js | getMyOrders | GET `/api/v1/account/orders` | GET `/api/v1/account/orders` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | quickReorder | POST `/api/v1/account/orders/{orderId}/quick-reorder` | POST `/api/v1/account/orders/{orderId}/quick-reorder` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | predictiveSearch | GET `/api/v1/search/predictive` | GET `/api/v1/search/predictive` | Keep | Uses `q` + `scope`. |
| storefront | frontend/storefront-noura/lib/api.js | getTrendTags | GET `/api/v1/search/trend-tags` | GET `/api/v1/search/trend-tags` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | getBestSellers | GET `/api/v1/recommendations/best-sellers` | GET `/api/v1/recommendations/best-sellers` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | getTrendingProducts | GET `/api/v1/recommendations/trending` | GET `/api/v1/recommendations/trending` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | getDeals | GET `/api/v1/recommendations/deals` | GET `/api/v1/recommendations/deals` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | getPersonalizedRecommendations | GET `/api/v1/recommendations/personalized` | GET `/api/v1/recommendations/personalized` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | getCrossSellProducts | GET `/api/v1/recommendations/cross-sell` | GET `/api/v1/recommendations/cross-sell` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | getProductReviews | GET `/api/v1/products/{productId}/reviews` | GET `/api/v1/products/{productId}/reviews` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | addProductReview | POST `/api/v1/products/{productId}/reviews` | POST `/api/v1/products/{productId}/reviews` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | getRelatedProducts | GET `/api/v1/products/{productId}/related` | GET `/api/v1/products/{productId}/related` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | getFrequentlyBoughtTogether | GET `/api/v1/products/{productId}/frequently-bought-together` | GET `/api/v1/products/{productId}/frequently-bought-together` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | getActivePromotions | GET `/api/v1/promotions/active` | GET `/api/v1/promotions/active` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | applyCoupon | POST `/api/v1/cart/coupon` | POST `/api/v1/cart/coupon` | Change | Payload now uses `{ couponCode }` per Swagger. |
| storefront | frontend/storefront-noura/lib/api.js | getCheckoutPreview | GET `/api/v1/checkout/steps/review` | GET `/api/v1/checkout/steps/review` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | submitCheckoutShipping | POST `/api/v1/checkout/steps/shipping` | POST `/api/v1/checkout/steps/shipping` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | submitCheckoutPayment | POST `/api/v1/checkout/steps/payment` | POST `/api/v1/checkout/steps/payment` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | confirmCheckout | POST `/api/v1/checkout/steps/confirm` | POST `/api/v1/checkout/steps/confirm` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | getOrderTimeline | GET `/api/v1/orders/{orderId}/timeline` | GET `/api/v1/orders/{orderId}/timeline` | Keep | Verify customer access vs admin-only. |
| storefront | frontend/storefront-noura/lib/api.js | getStores | GET `/api/v1/stores` | GET `/api/v1/stores` | Keep | Pageable params. |
| storefront | frontend/storefront-noura/lib/api.js | getNearestStores | GET `/api/v1/stores/nearest` | GET `/api/v1/stores/nearest` | Keep | Uses `lat`, `lng`, `limit`. |
| storefront | frontend/storefront-noura/lib/api.js | getMyNotifications | GET `/api/v1/notifications/me` | GET `/api/v1/notifications/me` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | getUnreadCount | GET `/api/v1/notifications/me/unread-count` | GET `/api/v1/notifications/me/unread-count` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | markNotificationRead | PATCH `/api/v1/notifications/{notificationId}/read` | PATCH `/api/v1/notifications/{notificationId}/read` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | markAllNotificationsRead | PATCH `/api/v1/notifications/me/read-all` | PATCH `/api/v1/notifications/me/read-all` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | getPaymentMethods | GET `/api/v1/account/payment-methods` | GET `/api/v1/account/payment-methods` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | addPaymentMethod | POST `/api/v1/account/payment-methods` | POST `/api/v1/account/payment-methods` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | deletePaymentMethod | DELETE `/api/v1/account/payment-methods/{paymentMethodId}` | DELETE `/api/v1/account/payment-methods/{paymentMethodId}` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | updateProfile | PUT `/api/v1/account/profile` | PUT `/api/v1/account/profile` | Change | Payload now uses `{ fullName, phone }` per Swagger. |
| storefront | frontend/storefront-noura/lib/api.js | requestPasswordReset | POST `/api/v1/auth/password-reset/request` | POST `/api/v1/auth/password-reset/request` | Keep | - |
| storefront | frontend/storefront-noura/lib/api.js | confirmPasswordReset | POST `/api/v1/auth/password-reset/confirm` | POST `/api/v1/auth/password-reset/confirm` | Keep | - |

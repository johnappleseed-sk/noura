function defaultBodyMode(method) {
  return method === 'GET' || method === 'DELETE' ? 'none' : 'json'
}

function endpoint(method, path, options = {}) {
  return {
    id: `${method} ${path}`,
    method,
    path,
    withAuth: options.withAuth ?? true,
    bodyMode: options.bodyMode || defaultBodyMode(method),
    defaultQuery: options.defaultQuery,
    defaultBody: options.defaultBody
  }
}

function group(key, label, description, endpoints) {
  return {
    key,
    label,
    description,
    endpoints: endpoints.map(([method, path, options]) => endpoint(method, path, options))
  }
}

export const endpointCatalog = [
  group("product-controller", "Product Controller", "Swagger tag: product-controller", [
    ["GET", "/api/inventory/v1/products", { defaultQuery: {"page": 0, "size": 20, "sortBy": "createdAt", "direction": "desc"} }],
    ["POST", "/api/inventory/v1/products"],
    ["DELETE", "/api/inventory/v1/products/{productId}"],
    ["GET", "/api/inventory/v1/products/{productId}"],
    ["PUT", "/api/inventory/v1/products/{productId}"],
    ["GET", "/api/v1/products", { defaultQuery: {"view": "grid", "page": 0, "size": 20, "sortBy": "createdAt", "direction": "desc"} }],
    ["POST", "/api/v1/products"],
    ["GET", "/api/v1/products/trend-tags"],
    ["DELETE", "/api/v1/products/{productId}"],
    ["GET", "/api/v1/products/{productId}"],
    ["PATCH", "/api/v1/products/{productId}"],
    ["PUT", "/api/v1/products/{productId}"],
    ["GET", "/api/v1/products/{productId}/frequently-bought-together"],
    ["GET", "/api/v1/products/{productId}/inventory"],
    ["PUT", "/api/v1/products/{productId}/inventory"],
    ["POST", "/api/v1/products/{productId}/media"],
    ["GET", "/api/v1/products/{productId}/related"],
    ["GET", "/api/v1/products/{productId}/reviews"],
    ["POST", "/api/v1/products/{productId}/reviews"],
    ["GET", "/api/v1/products/{productId}/variants"],
    ["POST", "/api/v1/products/{productId}/variants"],
  ]),
  group("catalog-management-controller", "Catalog Management Controller", "Swagger tag: catalog-management-controller", [
    ["POST", "/api/v1/attribute-sets"],
    ["POST", "/api/v1/attributes"],
    ["POST", "/api/v1/categories"],
    ["POST", "/api/v1/categories/ai/suggest"],
    ["GET", "/api/v1/categories/analytics"],
    ["GET", "/api/v1/categories/change-requests"],
    ["POST", "/api/v1/categories/change-requests"],
    ["PATCH", "/api/v1/categories/change-requests/{requestId}/approve"],
    ["PATCH", "/api/v1/categories/change-requests/{requestId}/reject"],
    ["POST", "/api/v1/categories/channel-mappings"],
    ["GET", "/api/v1/categories/tree"],
    ["PUT", "/api/v1/categories/{categoryId}"],
    ["GET", "/api/v1/categories/{categoryId}/channel-mappings"],
    ["GET", "/api/v1/categories/{categoryId}/translations"],
    ["PUT", "/api/v1/categories/{categoryId}/translations/{locale}"],
  ]),
  group("user-controller", "User Controller", "Swagger tag: user-controller", [
    ["GET", "/api/v1/account/addresses"],
    ["POST", "/api/v1/account/addresses"],
    ["DELETE", "/api/v1/account/addresses/{addressId}"],
    ["PUT", "/api/v1/account/addresses/{addressId}"],
    ["GET", "/api/v1/account/approvals"],
    ["PUT", "/api/v1/account/company-profile"],
    ["GET", "/api/v1/account/orders"],
    ["POST", "/api/v1/account/orders/{orderId}/quick-reorder"],
    ["GET", "/api/v1/account/payment-methods"],
    ["POST", "/api/v1/account/payment-methods"],
    ["DELETE", "/api/v1/account/payment-methods/{paymentMethodId}"],
    ["PUT", "/api/v1/account/payment-methods/{paymentMethodId}"],
    ["DELETE", "/api/v1/account/profile"],
    ["GET", "/api/v1/account/profile"],
    ["PUT", "/api/v1/account/profile"],
  ]),
  group("inventory-controller", "Inventory Controller", "Swagger tag: inventory-controller", [
    ["GET", "/api/v1/inventory/{variantId}"],
    ["POST", "/api/v1/inventory/adjust"],
    ["POST", "/api/v1/inventory/check"],
    ["POST", "/api/v1/inventory/confirm"],
    ["POST", "/api/v1/inventory/release"],
    ["POST", "/api/v1/inventory/reserve"],
    ["GET", "/api/v1/inventory/variants/{variantId}"],
    ["GET", "/api/v1/inventory/warehouses"],
    ["POST", "/api/v1/inventory/warehouses"],
  ]),
  group("stock-movement-controller", "Stock Movement Controller", "Swagger tag: stock-movement-controller", [
    ["GET", "/api/inventory/v1/movements", { defaultQuery: {"page": 0, "size": 20, "sortBy": "processedAt", "direction": "desc"} }],
    ["POST", "/api/inventory/v1/movements/adjustments"],
    ["POST", "/api/inventory/v1/movements/inbound"],
    ["POST", "/api/inventory/v1/movements/outbound"],
    ["POST", "/api/inventory/v1/movements/returns"],
    ["POST", "/api/inventory/v1/movements/transfers"],
    ["GET", "/api/inventory/v1/movements/{movementId}"],
  ]),
  group("warehouse-controller", "Warehouse Controller", "Swagger tag: warehouse-controller", [
    ["GET", "/api/inventory/v1/warehouses", { defaultQuery: {"page": 0, "size": 20, "sortBy": "name", "direction": "asc"} }],
    ["POST", "/api/inventory/v1/warehouses"],
    ["DELETE", "/api/inventory/v1/warehouses/{warehouseId}"],
    ["GET", "/api/inventory/v1/warehouses/{warehouseId}"],
    ["PUT", "/api/inventory/v1/warehouses/{warehouseId}"],
    ["GET", "/api/inventory/v1/warehouses/{warehouseId}/bins", { defaultQuery: {"page": 0, "size": 20, "sortBy": "binCode", "direction": "asc"} }],
    ["POST", "/api/inventory/v1/warehouses/{warehouseId}/bins"],
  ]),
  group("cart-controller", "Cart Controller", "Swagger tag: cart-controller", [
    ["GET", "/api/v1/cart"],
    ["POST", "/api/v1/cart/coupon"],
    ["DELETE", "/api/v1/cart/items"],
    ["POST", "/api/v1/cart/items"],
    ["DELETE", "/api/v1/cart/items/{cartItemId}"],
    ["PUT", "/api/v1/cart/items/{cartItemId}"],
  ]),
  group("category-controller", "Category Controller", "Swagger tag: category-controller", [
    ["GET", "/api/inventory/v1/categories", { defaultQuery: {"page": 0, "size": 20, "sortBy": "name", "direction": "asc"} }],
    ["POST", "/api/inventory/v1/categories"],
    ["GET", "/api/inventory/v1/categories/tree", { defaultQuery: {"activeOnly": true} }],
    ["DELETE", "/api/inventory/v1/categories/{categoryId}"],
    ["GET", "/api/inventory/v1/categories/{categoryId}"],
    ["PUT", "/api/inventory/v1/categories/{categoryId}"],
  ]),
  group("notification-controller", "Notification Controller", "Swagger tag: notification-controller", [
    ["POST", "/api/v1/notifications/broadcast"],
    ["PATCH", "/api/v1/notifications/me/read-all"],
    ["GET", "/api/v1/notifications/me"],
    ["GET", "/api/v1/notifications/me/unread-count"],
    ["PATCH", "/api/v1/notifications/{notificationId}/read"],
    ["POST", "/api/v1/notifications/user/{userId}"],
  ]),
  group("pricing-controller", "Pricing Controller", "Swagger tag: pricing-controller", [
    ["GET", "/api/v1/price-lists"],
    ["POST", "/api/v1/price-lists"],
    ["POST", "/api/v1/prices"],
    ["GET", "/api/v1/prices/variants/{variantId}"],
    ["GET", "/api/v1/promotions/active"],
    ["POST", "/api/v1/promotions"],
  ]),
  group("recommendation-controller", "Recommendation Controller", "Swagger tag: recommendation-controller", [
    ["GET", "/api/v1/recommendations/best-sellers"],
    ["GET", "/api/v1/recommendations/cross-sell"],
    ["GET", "/api/v1/recommendations/deals"],
    ["GET", "/api/v1/recommendations/mock-ai"],
    ["GET", "/api/v1/recommendations/personalized"],
    ["GET", "/api/v1/recommendations/trending"],
  ]),
  group("store-controller", "Store Controller", "Swagger tag: store-controller", [
    ["GET", "/api/v1/stores", { defaultQuery: {"page": 0, "size": 20, "sortBy": "name", "direction": "asc"} }],
    ["POST", "/api/v1/stores"],
    ["GET", "/api/v1/stores/nearest", { defaultQuery: {"lat": "example", "lng": "example", "limit": 5} }],
    ["PUT", "/api/v1/stores/preferred/{storeId}"],
    ["DELETE", "/api/v1/stores/{storeId}"],
    ["PUT", "/api/v1/stores/{storeId}"],
  ]),
  group("admin-dashboard-controller", "Admin Dashboard Controller", "Swagger tag: admin-dashboard-controller", [
    ["GET", "/api/v1/admin/b2b/approvals"],
    ["PATCH", "/api/v1/admin/b2b/approvals/{approvalId}"],
    ["GET", "/api/v1/admin/dashboard/summary"],
    ["GET", "/api/v1/admin/users", { defaultQuery: {"page": 0, "size": 20, "sortBy": "createdAt", "direction": "desc"} }],
    ["PATCH", "/api/v1/admin/users/{userId}"],
  ]),
  group("auth-controller", "Auth Controller", "Swagger tag: auth-controller", [
    ["POST", "/api/v1/auth/login", { withAuth: false, defaultBody: {"email": "admin@noura.local", "password": "Admin123!"} }],
    ["POST", "/api/v1/auth/password-reset/confirm", { withAuth: false }],
    ["POST", "/api/v1/auth/password-reset/request", { withAuth: false }],
    ["POST", "/api/v1/auth/refresh", { withAuth: false }],
    ["POST", "/api/v1/auth/register", { withAuth: false, defaultBody: {"email": "ops@noura.local", "password": "Admin123!", "fullName": "Ops Admin"} }],
  ]),
  group("checkout-controller", "Checkout Controller", "Swagger tag: checkout-controller", [
    ["POST", "/api/v1/checkout"],
    ["POST", "/api/v1/checkout/steps/confirm"],
    ["POST", "/api/v1/checkout/steps/payment"],
    ["GET", "/api/v1/checkout/steps/review"],
    ["POST", "/api/v1/checkout/steps/shipping"],
  ]),
  group("search-controller", "Search Controller", "Swagger tag: search-controller", [
    ["GET", "/api/v1/search/predictive", { defaultQuery: {"query": "example", "view": "grid"} }],
    ["GET", "/api/v1/search/trend-tags"],
  ]),
  group("inventory-reporting-controller", "Inventory Reporting Controller", "Swagger tag: inventory-reporting-controller", [
    ["GET", "/api/inventory/v1/reports/export", { defaultQuery: {"reportType": "example"} }],
    ["GET", "/api/inventory/v1/reports/low-stock"],
    ["GET", "/api/inventory/v1/reports/movement-history", { defaultQuery: {"page": 0, "size": 20, "sortBy": "processedAt", "direction": "desc"} }],
    ["GET", "/api/inventory/v1/reports/stock-valuation"],
    ["GET", "/api/inventory/v1/reports/turnover"],
  ]),
  group("webhook-subscription-controller", "Webhook Subscription Controller", "Swagger tag: webhook-subscription-controller", [
    ["GET", "/api/inventory/v1/webhooks"],
    ["POST", "/api/inventory/v1/webhooks"],
    ["DELETE", "/api/inventory/v1/webhooks/{subscriptionId}"],
    ["GET", "/api/inventory/v1/webhooks/{subscriptionId}"],
    ["PUT", "/api/inventory/v1/webhooks/{subscriptionId}"],
  ]),
  group("order-controller", "Order Controller", "Swagger tag: order-controller", [
    ["GET", "/api/v1/orders", { defaultQuery: {"page": 0, "size": 20, "sortBy": "createdAt", "direction": "desc"} }],
    ["GET", "/api/v1/orders/{orderId}"],
    ["PATCH", "/api/v1/orders/{orderId}/status"],
    ["GET", "/api/v1/orders/{orderId}/timeline"],
  ]),
  group("warehouse-bin-controller", "Warehouse Bin Controller", "Swagger tag: warehouse-bin-controller", [
    ["GET", "/api/inventory/v1/bins", { defaultQuery: {"page": 0, "size": 20, "sortBy": "binCode", "direction": "asc"} }],
    ["DELETE", "/api/inventory/v1/bins/{binId}"],
    ["GET", "/api/inventory/v1/bins/{binId}"],
    ["PUT", "/api/inventory/v1/bins/{binId}"],
  ]),
  group("inventory-auth-controller", "Inventory Auth Controller", "Swagger tag: inventory-auth-controller", [
    ["POST", "/api/inventory/v1/auth/login", { withAuth: false, defaultBody: {"username": "admin", "password": "Admin123!"} }],
    ["GET", "/api/inventory/v1/auth/me", { withAuth: false }],
    ["POST", "/api/inventory/v1/auth/register", { withAuth: false }],
  ]),
  group("product-variant-controller", "Product Variant Controller", "Swagger tag: product-variant-controller", [
    ["PUT", "/api/v1/variants/{variantId}"],
  ]),
  group("audit-log-controller", "Audit Log Controller", "Swagger tag: audit-log-controller", [
    ["GET", "/api/inventory/v1/audit-logs", { defaultQuery: {"page": 0, "size": 20, "sortBy": "createdAt", "direction": "desc"} }],
  ]),
  group("batch-lot-controller", "Batch Lot Controller", "Swagger tag: batch-lot-controller", [
    ["GET", "/api/inventory/v1/batches", { defaultQuery: {"page": 0, "size": 20, "sortBy": "expiryDate", "direction": "asc"} }],
    ["GET", "/api/inventory/v1/batches/{batchId}"],
  ]),
  group("serial-number-controller", "Serial Number Controller", "Swagger tag: serial-number-controller", [
    ["GET", "/api/inventory/v1/serials", { defaultQuery: {"page": 0, "size": 20, "sortBy": "updatedAt", "direction": "desc"} }],
    ["GET", "/api/inventory/v1/serials/{serialId}"],
  ]),
  group("inventory-barcode-controller", "Inventory Barcode Controller", "Swagger tag: inventory-barcode-controller", [
    ["GET", "/api/inventory/v1/barcodes/batches/{batchId}", { defaultQuery: {"qr": false, "width": 360, "height": 120} }],
    ["GET", "/api/inventory/v1/barcodes/bins/{binId}", { defaultQuery: {"qr": false, "width": 360, "height": 120} }],
    ["GET", "/api/inventory/v1/barcodes/products/{productId}", { defaultQuery: {"qr": false, "width": 360, "height": 120} }],
  ]),
  group("stock-level-controller", "Stock Level Controller", "Swagger tag: stock-level-controller", [
    ["GET", "/api/inventory/v1/stock-levels", { defaultQuery: {"page": 0, "size": 20, "sortBy": "updatedAt", "direction": "desc"} }],
  ]),
  group("system-controller", "System Controller", "Swagger tag: system-controller", [
    ["GET", "/api/inventory/v1/system/status"],
  ]),
]

export const endpointCount = endpointCatalog.reduce((total, groupItem) => total + groupItem.endpoints.length, 0)

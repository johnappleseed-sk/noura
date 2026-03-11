# Noura API Matrix (from Swagger)

Generated from `/v3/api-docs`.

## admin-dashboard-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/v1/admin/b2b/approvals | approvalQueue | - | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.user.ApprovalDto |
| PATCH | /api/v1/admin/b2b/approvals/{approvalId} | updateApproval | approvalId(path* string:uuid) | com.noura.platform.dto.user.ApprovalUpdateRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.user.ApprovalDto |
| GET | /api/v1/admin/dashboard/summary | summary | - | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.dashboard.DashboardSummaryDto |
| GET | /api/v1/admin/users | users | page(query integer:int32), size(query integer:int32), sortBy(query string), direction(query string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.common.api.PageResponseCom.noura.platform.dto.user.UserProfileDto |
| PATCH | /api/v1/admin/users/{userId} | updateUser | userId(path* string:uuid) | com.noura.platform.dto.user.AdminUserUpdateRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.user.UserProfileDto |

## audit-log-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/inventory/v1/audit-logs | listAuditLogs | entityType(query string), entityId(query string), actionCode(query string), actorEmail(query string), occurredFrom(query string:date-time), occurredTo(query string:date-time), page(query integer:int32), size(query integer:int32), sortBy(query string), direction(query string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.common.api.PageResponseCom.noura.platform.inventory.dto.audit.AuditLogResponse |

## auth-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| POST | /api/v1/auth/login | login | - | com.noura.platform.dto.auth.LoginRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.auth.AuthTokensResponse |
| POST | /api/v1/auth/password-reset/confirm | confirmReset | - | com.noura.platform.dto.auth.PasswordResetConfirmRequest | com.noura.platform.common.api.ApiResponseJava.lang.Void |
| POST | /api/v1/auth/password-reset/request | requestReset | - | com.noura.platform.dto.auth.PasswordResetRequest | com.noura.platform.common.api.ApiResponseJava.lang.Void |
| POST | /api/v1/auth/refresh | refresh | - | com.noura.platform.dto.auth.RefreshTokenRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.auth.AuthTokensResponse |
| POST | /api/v1/auth/register | register | - | com.noura.platform.dto.auth.RegisterRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.auth.AuthTokensResponse |

## batch-lot-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/inventory/v1/batches | listBatches | productId(query string), status(query string), expiringBefore(query string:date), expiringAfter(query string:date), page(query integer:int32), size(query integer:int32), sortBy(query string), direction(query string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.common.api.PageResponseCom.noura.platform.inventory.dto.batch.BatchLotResponse |
| GET | /api/inventory/v1/batches/{batchId} | getBatch | batchId(path* string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.batch.BatchLotResponse |

## cart-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/v1/cart | myCart | - | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.cart.CartDto |
| POST | /api/v1/cart/coupon | applyCoupon | - | com.noura.platform.dto.cart.ApplyCouponRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.cart.CartDto |
| DELETE | /api/v1/cart/items | clearItems | - | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.cart.CartDto |
| POST | /api/v1/cart/items | addItem | - | com.noura.platform.dto.cart.AddCartItemRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.cart.CartDto |
| DELETE | /api/v1/cart/items/{cartItemId} | removeItem | cartItemId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.cart.CartDto |
| PUT | /api/v1/cart/items/{cartItemId} | updateItem | cartItemId(path* string:uuid) | com.noura.platform.dto.cart.UpdateCartItemRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.cart.CartDto |

## catalog-management-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| POST | /api/v1/attribute-sets | createAttributeSet | - | com.noura.platform.dto.catalog.AttributeSetRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.catalog.AttributeSetDto |
| POST | /api/v1/attributes | createAttribute | - | com.noura.platform.dto.catalog.AttributeRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.catalog.AttributeDto |
| POST | /api/v1/categories | createCategory | - | com.noura.platform.dto.catalog.CategoryRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.catalog.CategoryDto |
| POST | /api/v1/categories/ai/suggest | suggestCategory | - | com.noura.platform.dto.catalog.CategorySuggestionRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.catalog.CategorySuggestionResponse |
| GET | /api/v1/categories/analytics | categoryAnalytics | from(query string:date-time), to(query string:date-time) | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.catalog.CategoryAnalyticsDto |
| GET | /api/v1/categories/change-requests | categoryChangeRequests | status(query string) | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.catalog.CategoryChangeRequestDto |
| POST | /api/v1/categories/change-requests | submitCategoryChangeRequest | - | com.noura.platform.dto.catalog.CategoryChangeSubmitRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.catalog.CategoryChangeRequestDto |
| PATCH | /api/v1/categories/change-requests/{requestId}/approve | approveCategoryChangeRequest | requestId(path* string:uuid) | com.noura.platform.dto.catalog.CategoryChangeReviewRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.catalog.CategoryChangeRequestDto |
| PATCH | /api/v1/categories/change-requests/{requestId}/reject | rejectCategoryChangeRequest | requestId(path* string:uuid) | com.noura.platform.dto.catalog.CategoryChangeReviewRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.catalog.CategoryChangeRequestDto |
| POST | /api/v1/categories/channel-mappings | createChannelCategoryMapping | - | com.noura.platform.dto.catalog.ChannelCategoryMappingRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.catalog.ChannelCategoryMappingDto |
| GET | /api/v1/categories/tree | categoryTree | locale(query string) | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.catalog.CategoryTreeDto |
| PUT | /api/v1/categories/{categoryId} | updateCategory | categoryId(path* string:uuid) | com.noura.platform.dto.catalog.CategoryUpdateRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.catalog.CategoryDto |
| GET | /api/v1/categories/{categoryId}/channel-mappings | categoryChannelMappings | categoryId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.catalog.ChannelCategoryMappingDto |
| GET | /api/v1/categories/{categoryId}/translations | categoryTranslations | categoryId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.catalog.CategoryTranslationDto |
| PUT | /api/v1/categories/{categoryId}/translations/{locale} | upsertCategoryTranslation | categoryId(path* string:uuid), locale(path* string) | com.noura.platform.dto.catalog.CategoryTranslationRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.catalog.CategoryTranslationDto |

## category-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/inventory/v1/categories | listCategories | query(query string), parentId(query string), active(query boolean), page(query integer:int32), size(query integer:int32), sortBy(query string), direction(query string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.common.api.PageResponseCom.noura.platform.inventory.dto.category.CategoryResponse |
| POST | /api/inventory/v1/categories | createCategory_1 | - | com.noura.platform.inventory.dto.category.CategoryRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.category.CategoryResponse |
| GET | /api/inventory/v1/categories/tree | categoryTree_1 | activeOnly(query boolean) | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.inventory.dto.category.CategoryTreeResponse |
| DELETE | /api/inventory/v1/categories/{categoryId} | deleteCategory | categoryId(path* string) | - | com.noura.platform.common.api.ApiResponseJava.lang.Void |
| GET | /api/inventory/v1/categories/{categoryId} | getCategory | categoryId(path* string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.category.CategoryResponse |
| PUT | /api/inventory/v1/categories/{categoryId} | updateCategory_1 | categoryId(path* string) | com.noura.platform.inventory.dto.category.CategoryRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.category.CategoryResponse |

## checkout-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| POST | /api/v1/checkout | directCheckout | - | com.noura.platform.dto.order.CheckoutRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.order.OrderDto |
| POST | /api/v1/checkout/steps/confirm | confirm_1 | - | com.noura.platform.dto.order.CheckoutRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.order.OrderDto |
| POST | /api/v1/checkout/steps/payment | paymentStep | - | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.order.CheckoutStepPreviewDto |
| GET | /api/v1/checkout/steps/review | reviewStep | - | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.order.CheckoutStepPreviewDto |
| POST | /api/v1/checkout/steps/shipping | shippingStep | - | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.order.CheckoutStepPreviewDto |

## inventory-auth-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| POST | /api/inventory/v1/auth/login | login_1 | - | com.noura.platform.inventory.dto.auth.InventoryLoginRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.auth.InventoryAuthResponse |
| GET | /api/inventory/v1/auth/me | currentUser | - | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.auth.InventoryCurrentUserResponse |
| POST | /api/inventory/v1/auth/register | register_1 | - | com.noura.platform.inventory.dto.auth.InventoryRegisterRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.auth.InventoryAuthResponse |

## inventory-barcode-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/inventory/v1/barcodes/batches/{batchId} | batchBarcode | batchId(path* string), qr(query boolean), width(query integer:int32), height(query integer:int32) | - | string |
| GET | /api/inventory/v1/barcodes/bins/{binId} | binBarcode | binId(path* string), qr(query boolean), width(query integer:int32), height(query integer:int32) | - | string |
| GET | /api/inventory/v1/barcodes/products/{productId} | productBarcode | productId(path* string), qr(query boolean), width(query integer:int32), height(query integer:int32) | - | string |

## inventory-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| POST | /api/v1/inventory/adjust | adjust | - | com.noura.platform.dto.inventory.InventoryAdjustRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.inventory.InventoryLevelDto |
| POST | /api/v1/inventory/check | check | - | com.noura.platform.dto.inventory.InventoryCheckRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.inventory.InventoryCheckResultDto |
| POST | /api/v1/inventory/confirm | confirm | - | com.noura.platform.dto.inventory.InventoryReservationActionRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.inventory.InventoryReservationDto |
| POST | /api/v1/inventory/release | release | - | com.noura.platform.dto.inventory.InventoryReservationActionRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.inventory.InventoryReservationDto |
| POST | /api/v1/inventory/reserve | reserve | - | com.noura.platform.dto.inventory.InventoryReserveRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.inventory.InventoryReservationDto |
| GET | /api/v1/inventory/variants/{variantId} | stock_1 | variantId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.inventory.InventorySummaryDto |
| GET | /api/v1/inventory/warehouses | warehouses | - | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.inventory.WarehouseDto |
| POST | /api/v1/inventory/warehouses | createWarehouse | - | com.noura.platform.dto.inventory.WarehouseRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.inventory.WarehouseDto |
| GET | /api/v1/inventory/{variantId} | stock | variantId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.inventory.InventorySummaryDto |

## location-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| POST | /api/v1/location/forward-geocode | forwardGeocode | - | com.noura.platform.dto.location.ForwardGeocodeRequest | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.location.GeocodeResultDto |
| GET | /api/v1/location/nearby-stores | nearbyStores | lat(query* number), lng(query* number), serviceType(query string), openNow(query boolean), limit(query integer:int32), maxDistanceMeters(query integer:int32) | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.location.NearbyStoreDto |
| POST | /api/v1/location/resolve | resolve | - | com.noura.platform.dto.location.LocationResolveRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.location.LocationResolveDto |
| POST | /api/v1/location/reverse-geocode | reverseGeocode | - | com.noura.platform.dto.location.ReverseGeocodeRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.location.GeocodeResultDto |
| POST | /api/v1/location/validate-service-area | validateServiceArea | - | com.noura.platform.dto.location.ServiceAreaValidationRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.location.ServiceEligibilityDto |

## inventory-reporting-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/inventory/v1/reports/export | exportCsv | reportType(query* string), warehouseId(query string), productId(query string), dateFrom(query string:date-time), dateTo(query string:date-time) | - | string |
| GET | /api/inventory/v1/reports/low-stock | lowStock | warehouseId(query string) | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.inventory.dto.report.LowStockReportItemResponse |
| GET | /api/inventory/v1/reports/movement-history | movementHistory | movementType(query string), movementStatus(query string), warehouseId(query string), productId(query string), referenceQuery(query string), processedFrom(query string:date-time), processedTo(query string:date-time), page(query integer:int32), size(query integer:int32), sortBy(query string), direction(query string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.common.api.PageResponseCom.noura.platform.inventory.dto.stock.StockMovementResponse |
| GET | /api/inventory/v1/reports/stock-valuation | stockValuation | warehouseId(query string), productId(query string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.report.StockValuationReportResponse |
| GET | /api/inventory/v1/reports/turnover | turnover | dateFrom(query string:date-time), dateTo(query string:date-time) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.report.InventoryTurnoverReportResponse |

## notification-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| POST | /api/v1/notifications/broadcast | broadcast | - | com.noura.platform.dto.notification.SendNotificationRequest | com.noura.platform.common.api.ApiResponseJava.lang.Void |
| GET | /api/v1/notifications/me | myNotifications | - | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.notification.NotificationDto |
| PATCH | /api/v1/notifications/me/read-all | markAllAsRead | - | - | com.noura.platform.common.api.ApiResponseJava.lang.Integer |
| GET | /api/v1/notifications/me/unread-count | unreadCount | - | - | com.noura.platform.common.api.ApiResponseJava.lang.Long |
| POST | /api/v1/notifications/user/{userId} | pushToUser | userId(path* string:uuid) | com.noura.platform.dto.notification.SendNotificationRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.notification.NotificationDto |
| PATCH | /api/v1/notifications/{notificationId}/read | markAsRead | notificationId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.notification.NotificationDto |

## order-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/v1/orders | adminOrders | page(query integer:int32), size(query integer:int32), sortBy(query string), direction(query string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.common.api.PageResponseCom.noura.platform.dto.order.OrderDto |
| GET | /api/v1/orders/{orderId} | getById | orderId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.order.OrderDto |
| PATCH | /api/v1/orders/{orderId}/status | updateStatus | orderId(path* string:uuid) | com.noura.platform.dto.order.UpdateOrderStatusRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.order.OrderDto |
| GET | /api/v1/orders/{orderId}/timeline | orderTimeline | orderId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.order.OrderTimelineEventDto |

## service-area-admin-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| POST | /api/v1/admin/service-areas | create | - | com.noura.platform.dto.location.ServiceAreaRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.location.ServiceAreaDto |
| GET | /api/v1/admin/service-areas | list | query(query string), status(query string), type(query string), page(query integer:int32), size(query integer:int32), sortBy(query string), direction(query string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.common.api.PageResponseCom.noura.platform.dto.location.ServiceAreaDto |
| POST | /api/v1/admin/service-areas/validate | validate | - | com.noura.platform.dto.location.ServiceAreaValidationRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.location.ServiceEligibilityDto |
| POST | /api/v1/admin/service-areas/{serviceAreaId}/activate | activate | serviceAreaId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.location.ServiceAreaDto |
| POST | /api/v1/admin/service-areas/{serviceAreaId}/deactivate | deactivate | serviceAreaId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.location.ServiceAreaDto |
| DELETE | /api/v1/admin/service-areas/{serviceAreaId} | delete | serviceAreaId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseJava.lang.Void |
| GET | /api/v1/admin/service-areas/{serviceAreaId} | get | serviceAreaId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.location.ServiceAreaDto |
| PUT | /api/v1/admin/service-areas/{serviceAreaId} | update | serviceAreaId(path* string:uuid) | com.noura.platform.dto.location.ServiceAreaRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.location.ServiceAreaDto |

## admin-store-location-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/v1/admin/stores/{storeId}/location | get | storeId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.location.StoreLocationDto |
| PUT | /api/v1/admin/stores/{storeId}/location | update | storeId(path* string:uuid) | com.noura.platform.dto.location.StoreLocationRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.location.StoreLocationDto |

## pricing-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/v1/price-lists | priceLists | - | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.pricing.PriceListDto |
| POST | /api/v1/price-lists | createPriceList | - | com.noura.platform.dto.pricing.PriceListRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.pricing.PriceListDto |
| POST | /api/v1/prices | upsertPrice | - | com.noura.platform.dto.pricing.PriceUpsertRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.pricing.PriceDto |
| GET | /api/v1/prices/variants/{variantId} | quoteVariantPrice | variantId(path* string:uuid), customerGroupId(query string:uuid), channelId(query string:uuid) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.pricing.PriceQuoteDto |
| POST | /api/v1/promotions | createPromotion | - | com.noura.platform.dto.pricing.PromotionCreateRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.pricing.PromotionDto |
| GET | /api/v1/promotions/active | activePromotions | - | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.pricing.PromotionDto |

## product-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/inventory/v1/products | listProducts_1 | query(query string), categoryId(query string), active(query boolean), page(query integer:int32), size(query integer:int32), sortBy(query string), direction(query string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.common.api.PageResponseCom.noura.platform.inventory.dto.product.ProductResponse |
| POST | /api/inventory/v1/products | createProduct | - | com.noura.platform.inventory.dto.product.ProductRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.product.ProductResponse |
| DELETE | /api/inventory/v1/products/{productId} | deleteProduct | productId(path* string) | - | com.noura.platform.common.api.ApiResponseJava.lang.Void |
| GET | /api/inventory/v1/products/{productId} | getProduct_1 | productId(path* string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.product.ProductResponse |
| PUT | /api/inventory/v1/products/{productId} | updateProduct | productId(path* string) | com.noura.platform.inventory.dto.product.ProductRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.product.ProductResponse |
| GET | /api/v1/products | listProducts | query(query string), category(query string), categoryId(query string:uuid), brand(query string), minPrice(query number), maxPrice(query number), minRating(query number:double), storeId(query string:uuid), availableAtStore(query boolean), flashSale(query boolean), trending(query boolean), attributeKey(query string), attributeValue(query string), view(query string), page(query integer:int32), size(query integer:int32), sortBy(query string), direction(query string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.common.api.PageResponseCom.noura.platform.dto.product.ProductDto |
| POST | /api/v1/products | create_1 | - | com.noura.platform.dto.product.ProductRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.product.ProductDto |
| GET | /api/v1/products/trend-tags | trendTags_1 | - | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.product.TrendTagDto |
| DELETE | /api/v1/products/{productId} | delete_1 | productId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseJava.lang.Void |
| GET | /api/v1/products/{productId} | getProduct | productId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.product.ProductDto |
| PATCH | /api/v1/products/{productId} | patch | productId(path* string:uuid) | com.noura.platform.dto.product.ProductPatchRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.product.ProductDto |
| PUT | /api/v1/products/{productId} | update_2 | productId(path* string:uuid) | com.noura.platform.dto.product.ProductRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.product.ProductDto |
| GET | /api/v1/products/{productId}/frequently-bought-together | frequentlyBoughtTogether | productId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.product.ProductDto |
| GET | /api/v1/products/{productId}/inventory | inventories | productId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.product.ProductInventoryDto |
| PUT | /api/v1/products/{productId}/inventory | upsertInventory | productId(path* string:uuid) | com.noura.platform.dto.product.ProductInventoryRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.product.ProductInventoryDto |
| POST | /api/v1/products/{productId}/media | addMedia | productId(path* string:uuid) | com.noura.platform.dto.product.ProductMediaRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.product.ProductMediaDto |
| GET | /api/v1/products/{productId}/related | related | productId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.product.ProductDto |
| GET | /api/v1/products/{productId}/reviews | reviews | productId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.product.ProductReviewDto |
| POST | /api/v1/products/{productId}/reviews | addReview | productId(path* string:uuid) | com.noura.platform.dto.product.ProductReviewRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.product.ProductReviewDto |
| GET | /api/v1/products/{productId}/variants | variants | productId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.product.ProductVariantDto |
| POST | /api/v1/products/{productId}/variants | addVariant | productId(path* string:uuid) | com.noura.platform.dto.product.ProductVariantRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.product.ProductVariantDto |

## product-variant-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| PUT | /api/v1/variants/{variantId} | update | variantId(path* string:uuid) | com.noura.platform.dto.product.ProductVariantRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.product.ProductVariantDto |

## recommendation-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/v1/recommendations/best-sellers | bestSellers | - | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.product.ProductDto |
| GET | /api/v1/recommendations/cross-sell | crossSell | - | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.product.ProductDto |
| GET | /api/v1/recommendations/deals | deals | - | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.product.ProductDto |
| GET | /api/v1/recommendations/mock-ai | mockAi | - | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.product.AiRecommendationResponse |
| GET | /api/v1/recommendations/personalized | personalized | - | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.product.ProductDto |
| GET | /api/v1/recommendations/trending | trending | - | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.product.ProductDto |

## search-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/v1/search/predictive | predictive | q(query* string), scope(query string) | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.product.SearchSuggestionDto |
| GET | /api/v1/search/trend-tags | trendTags | - | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.product.TrendTagDto |

## serial-number-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/inventory/v1/serials | listSerials | query(query string), productId(query string), serialStatus(query string), warehouseId(query string), binId(query string), batchId(query string), page(query integer:int32), size(query integer:int32), sortBy(query string), direction(query string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.common.api.PageResponseCom.noura.platform.inventory.dto.serial.SerialNumberResponse |
| GET | /api/inventory/v1/serials/{serialId} | getSerial | serialId(path* string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.serial.SerialNumberResponse |

## stock-level-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/inventory/v1/stock-levels | listStockLevels | productId(query string), warehouseId(query string), binId(query string), batchId(query string), lowStockOnly(query boolean), page(query integer:int32), size(query integer:int32), sortBy(query string), direction(query string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.common.api.PageResponseCom.noura.platform.inventory.dto.stock.StockLevelResponse |

## stock-movement-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/inventory/v1/movements | listMovements | movementType(query string), movementStatus(query string), warehouseId(query string), productId(query string), referenceQuery(query string), processedFrom(query string:date-time), processedTo(query string:date-time), page(query integer:int32), size(query integer:int32), sortBy(query string), direction(query string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.common.api.PageResponseCom.noura.platform.inventory.dto.stock.StockMovementResponse |
| POST | /api/inventory/v1/movements/adjustments | adjustStock | - | com.noura.platform.inventory.dto.stock.AdjustmentMovementRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.stock.StockMovementResponse |
| POST | /api/inventory/v1/movements/inbound | receiveInbound | - | com.noura.platform.inventory.dto.stock.InboundMovementRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.stock.StockMovementResponse |
| POST | /api/inventory/v1/movements/outbound | shipOutbound | - | com.noura.platform.inventory.dto.stock.OutboundMovementRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.stock.StockMovementResponse |
| POST | /api/inventory/v1/movements/returns | returnStock | - | com.noura.platform.inventory.dto.stock.ReturnMovementRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.stock.StockMovementResponse |
| POST | /api/inventory/v1/movements/transfers | transferStock | - | com.noura.platform.inventory.dto.stock.TransferMovementRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.stock.StockMovementResponse |
| GET | /api/inventory/v1/movements/{movementId} | getMovement | movementId(path* string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.stock.StockMovementResponse |

## store-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/v1/stores | list | service(query string), openNow(query boolean), page(query integer:int32), size(query integer:int32), sortBy(query string), direction(query string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.common.api.PageResponseCom.noura.platform.dto.store.StoreDto |
| POST | /api/v1/stores | create | - | com.noura.platform.dto.store.StoreRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.store.StoreDto |
| GET | /api/v1/stores/nearest | nearest | lat(query* number), lng(query* number), limit(query integer:int32) | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.store.StoreDto |
| PUT | /api/v1/stores/preferred/{storeId} | setPreferredStore | storeId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseJava.lang.Void |
| DELETE | /api/v1/stores/{storeId} | delete | storeId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseJava.lang.Void |
| PUT | /api/v1/stores/{storeId} | update_1 | storeId(path* string:uuid) | com.noura.platform.dto.store.StoreRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.store.StoreDto |

## system-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/inventory/v1/system/status | status | - | - | com.noura.platform.inventory.api.SystemController$InventorySystemStatusResponse |

## user-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/v1/account/addresses | addresses | - | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.user.AddressDto |
| POST | /api/v1/account/addresses | addAddress | - | com.noura.platform.dto.user.AddressRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.user.AddressDto |
| DELETE | /api/v1/account/addresses/{addressId} | deleteAddress | addressId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseJava.lang.Void |
| GET | /api/v1/account/addresses/{addressId} | getAddress | addressId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.user.AddressDto |
| POST | /api/v1/account/addresses/{addressId}/set-default | setDefaultAddress | addressId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.user.AddressDto |
| PUT | /api/v1/account/addresses/{addressId} | updateAddress | addressId(path* string:uuid) | com.noura.platform.dto.user.AddressRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.user.AddressDto |
| GET | /api/v1/account/approvals | myApprovals | - | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.user.ApprovalDto |
| PUT | /api/v1/account/company-profile | upsertCompanyProfile | - | com.noura.platform.dto.user.CompanyProfileRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.user.CompanyProfileDto |
| GET | /api/v1/account/orders | orderHistory | - | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.order.OrderDto |
| POST | /api/v1/account/orders/{orderId}/quick-reorder | quickReorder | orderId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.order.OrderDto |
| GET | /api/v1/account/payment-methods | paymentMethods | - | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.dto.user.PaymentMethodDto |
| POST | /api/v1/account/payment-methods | addPaymentMethod | - | com.noura.platform.dto.user.PaymentMethodRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.user.PaymentMethodDto |
| DELETE | /api/v1/account/payment-methods/{paymentMethodId} | deletePaymentMethod | paymentMethodId(path* string:uuid) | - | com.noura.platform.common.api.ApiResponseJava.lang.Void |
| PUT | /api/v1/account/payment-methods/{paymentMethodId} | updatePaymentMethod | paymentMethodId(path* string:uuid) | com.noura.platform.dto.user.PaymentMethodRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.user.PaymentMethodDto |
| DELETE | /api/v1/account/profile | deleteAccount | - | - | com.noura.platform.common.api.ApiResponseJava.lang.Void |
| GET | /api/v1/account/profile | profile | - | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.user.UserProfileDto |
| PUT | /api/v1/account/profile | updateProfile | - | com.noura.platform.dto.user.UpdateProfileRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.dto.user.UserProfileDto |

## warehouse-bin-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/inventory/v1/bins | listBins | warehouseId(query string), query(query string), zoneCode(query string), active(query boolean), page(query integer:int32), size(query integer:int32), sortBy(query string), direction(query string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.common.api.PageResponseCom.noura.platform.inventory.dto.warehouse.WarehouseBinResponse |
| DELETE | /api/inventory/v1/bins/{binId} | deleteBin | binId(path* string) | - | com.noura.platform.common.api.ApiResponseJava.lang.Void |
| GET | /api/inventory/v1/bins/{binId} | getBin | binId(path* string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.warehouse.WarehouseBinResponse |
| PUT | /api/inventory/v1/bins/{binId} | updateBin | binId(path* string) | com.noura.platform.inventory.dto.warehouse.WarehouseBinRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.warehouse.WarehouseBinResponse |

## warehouse-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/inventory/v1/warehouses | listWarehouses | query(query string), active(query boolean), page(query integer:int32), size(query integer:int32), sortBy(query string), direction(query string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.common.api.PageResponseCom.noura.platform.inventory.dto.warehouse.WarehouseResponse |
| POST | /api/inventory/v1/warehouses | createWarehouse_1 | - | com.noura.platform.inventory.dto.warehouse.WarehouseRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.warehouse.WarehouseResponse |
| DELETE | /api/inventory/v1/warehouses/{warehouseId} | deleteWarehouse | warehouseId(path* string) | - | com.noura.platform.common.api.ApiResponseJava.lang.Void |
| GET | /api/inventory/v1/warehouses/{warehouseId} | getWarehouse | warehouseId(path* string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.warehouse.WarehouseResponse |
| PUT | /api/inventory/v1/warehouses/{warehouseId} | updateWarehouse | warehouseId(path* string) | com.noura.platform.inventory.dto.warehouse.WarehouseRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.warehouse.WarehouseResponse |
| GET | /api/inventory/v1/warehouses/{warehouseId}/bins | listWarehouseBins | warehouseId(path* string), query(query string), zoneCode(query string), active(query boolean), page(query integer:int32), size(query integer:int32), sortBy(query string), direction(query string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.common.api.PageResponseCom.noura.platform.inventory.dto.warehouse.WarehouseBinResponse |
| POST | /api/inventory/v1/warehouses/{warehouseId}/bins | createWarehouseBin | warehouseId(path* string) | com.noura.platform.inventory.dto.warehouse.WarehouseBinRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.warehouse.WarehouseBinResponse |

## webhook-subscription-controller

| Method | Path | Summary | Params | Request Body | Response |
| --- | --- | --- | --- | --- | --- |
| GET | /api/inventory/v1/webhooks | listSubscriptions | - | - | com.noura.platform.common.api.ApiResponseJava.util.ListCom.noura.platform.inventory.dto.webhook.WebhookSubscriptionResponse |
| POST | /api/inventory/v1/webhooks | createSubscription | - | com.noura.platform.inventory.dto.webhook.WebhookSubscriptionRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.webhook.WebhookSubscriptionResponse |
| DELETE | /api/inventory/v1/webhooks/{subscriptionId} | deleteSubscription | subscriptionId(path* string) | - | com.noura.platform.common.api.ApiResponseJava.lang.Void |
| GET | /api/inventory/v1/webhooks/{subscriptionId} | getSubscription | subscriptionId(path* string) | - | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.webhook.WebhookSubscriptionResponse |
| PUT | /api/inventory/v1/webhooks/{subscriptionId} | updateSubscription | subscriptionId(path* string) | com.noura.platform.inventory.dto.webhook.WebhookSubscriptionRequest | com.noura.platform.common.api.ApiResponseCom.noura.platform.inventory.dto.webhook.WebhookSubscriptionResponse |

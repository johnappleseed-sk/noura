# API Reference

## Base Information

- Base path: `/api/v1`
- Content type: `application/json`
- Auth header for protected routes: `Authorization: Bearer <access-token>`
- Swagger UI: `/swagger-ui`

## Response Envelope

Success:

```json
{
  "success": true,
  "message": "Operation message",
  "data": {},
  "timestamp": "2026-03-05T10:00:00Z",
  "path": "/api/v1/example"
}
```

Error:

```json
{
  "success": false,
  "message": "Error message",
  "error": {
    "code": "ERROR_CODE",
    "detail": "Detailed explanation"
  },
  "timestamp": "2026-03-05T10:00:00Z",
  "path": "/api/v1/example"
}
```

## Authentication and Roles

- `Public`: no token required
- `Authenticated`: valid JWT required
- `ADMIN`: user must have admin role
- `ADMIN or B2B`: user must have either role

---

## Auth Endpoints

| Method | URL | Auth | Description | Request Example | Response Example |
|---|---|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | Register user and issue tokens | `{"fullName":"Jane Doe","email":"jane@example.com","password":"StrongPass123"}` | `{"success":true,"data":{"userId":"...","roles":["CUSTOMER"],"accessToken":"...","refreshToken":"..."}}` |
| POST | `/api/v1/auth/login` | Public | Login and issue tokens | `{"email":"jane@example.com","password":"StrongPass123"}` | `{"success":true,"data":{"userId":"...","roles":["CUSTOMER"],"accessToken":"...","refreshToken":"..."}}` |
| POST | `/api/v1/auth/refresh` | Public | Rotate refresh token and issue new pair | `{"refreshToken":"uuid-token"}` | `{"success":true,"data":{"accessToken":"...","refreshToken":"..."}}` |
| POST | `/api/v1/auth/password-reset/request` | Public | Generate password reset token (silent for unknown email) | `{"email":"jane@example.com"}` | `{"success":true,"message":"If account exists, reset token was generated","data":null}` |
| POST | `/api/v1/auth/password-reset/confirm` | Public | Confirm password reset | `{"token":"reset-token","newPassword":"NewStrongPass123"}` | `{"success":true,"message":"Password updated","data":null}` |

---

## Product Endpoints

| Method | URL | Auth | Description | Request Example | Response Example |
|---|---|---|---|---|---|
| GET | `/api/v1/products` | Public | List products with filters and pagination | `?query=laptop&page=0&size=20&sortBy=createdAt&direction=desc` | `{"success":true,"data":{"content":[{"id":"...","name":"..." }],"page":0,"size":20}}` |
| GET | `/api/v1/products/trend-tags` | Public | List trend tags | none | `{"success":true,"data":[{"value":"flash-sale","score":97}]}` |
| GET | `/api/v1/products/{productId}` | Public | Get product details | path: `productId=uuid` | `{"success":true,"data":{"id":"...","name":"...","storeInventory":[...]}}` |
| POST | `/api/v1/products` | ADMIN | Create product | `{"name":"Laptop X","category":"Electronics","brand":"Acme","price":899.99,"flashSale":false,"trending":true,"bestSeller":false,"variants":[],"media":[],"inventory":[]}` | `{"success":true,"message":"Product created","data":{"id":"...","name":"Laptop X"}}` |
| PUT | `/api/v1/products/{productId}` | ADMIN | Update product | same as create payload | `{"success":true,"message":"Product updated","data":{"id":"...","name":"Laptop X"}}` |
| DELETE | `/api/v1/products/{productId}` | ADMIN | Delete product | none | `{"success":true,"message":"Product deleted","data":null}` |
| GET | `/api/v1/products/{productId}/reviews` | Public | List product reviews | path: `productId=uuid` | `{"success":true,"data":[{"id":"...","rating":5,"comment":"Great"}]}` |
| POST | `/api/v1/products/{productId}/reviews` | Authenticated | Add review | `{"rating":5,"comment":"Great quality"}` | `{"success":true,"message":"Review added","data":{"id":"...","rating":5}}` |
| POST | `/api/v1/products/{productId}/variants` | ADMIN | Add product variant | `{"color":"Black","size":"M","sku":"SKU-123"}` | `{"success":true,"data":{"id":"...","sku":"SKU-123"}}` |
| GET | `/api/v1/products/{productId}/variants` | Public | List variants for product | path: `productId=uuid` | `{"success":true,"data":[{"id":"...","sku":"SKU-123"}]}` |
| PUT | `/api/v1/variants/{variantId}` | ADMIN | Full update variant | `{"sku":"SKU-123","attributes":{"color":"Black","size":"M"},"price":89.99,"stock":50}` | `{"success":true,"data":{"id":"...","sku":"SKU-123"}}` |
| POST | `/api/v1/products/{productId}/media` | ADMIN | Add product media | `{"mediaType":"IMAGE","url":"https://cdn/img.jpg","sortOrder":0}` | `{"success":true,"data":{"id":"...","url":"https://cdn/img.jpg"}}` |
| PUT | `/api/v1/products/{productId}/inventory` | ADMIN | Upsert store inventory | `{"storeId":"uuid","stock":30,"storePrice":849.99}` | `{"success":true,"data":{"id":"...","stock":30,"storePrice":849.99}}` |
| PATCH | `/api/v1/products/{productId}` | ADMIN | Partial update product metadata/SEO/attributes/flags | `{"seo":{"slug":"new-slug","metaTitle":"New Title","metaDescription":"New Desc"},"active":true}` | `{"success":true,"message":"Product patched","data":{"id":"...","seo":{"slug":"new-slug"}}}` |
| GET | `/api/v1/products/{productId}/inventory` | Public | Get all store inventory for product | path: `productId=uuid` | `{"success":true,"data":[{"storeId":"...","stock":30}]}` |
| GET | `/api/v1/products/{productId}/related` | Public | Related products by category/brand/popularity | path: `productId=uuid` | `{"success":true,"data":[{"id":"...","name":"Related A"}]}` |
| GET | `/api/v1/products/{productId}/frequently-bought-together` | Public | Frequently bought together suggestions | path: `productId=uuid` | `{"success":true,"data":[{"id":"...","name":"Bundle Item"}]}` |

---

## Category and Taxonomy Endpoints

| Method | URL | Auth | Description | Request Example | Response Example |
|---|---|---|---|---|---|
| POST | `/api/v1/categories` | ADMIN | Create category (supports parent category) | `{"name":"Laptops","description":"Portable computers","parentId":"uuid"}` | `{"success":true,"data":{"id":"...","name":"Laptops","parentId":"uuid"}}` |
| GET | `/api/v1/categories/tree` | Public | Retrieve full category hierarchy tree | none | `{"success":true,"data":[{"id":"...","name":"Electronics","children":[...]}]}` |
| POST | `/api/v1/attributes` | ADMIN | Create product attribute definition | `{"name":"screenSize","type":"NUMBER","possibleValues":[]}` | `{"success":true,"data":{"id":"...","name":"screenSize","type":"NUMBER"}}` |
| POST | `/api/v1/attribute-sets` | ADMIN | Create reusable attribute set | `{"name":"Laptop Specs","attributeIds":["uuid-1","uuid-2"]}` | `{"success":true,"data":{"id":"...","name":"Laptop Specs","attributes":[...]}}` |

---

## Inventory Endpoints

| Method | URL | Auth | Description | Request Example | Response Example |
|---|---|---|---|---|---|
| GET | `/api/v1/inventory/{variantId}` | Authenticated | Get inventory summary for variant across warehouses | path: `variantId=uuid` | `{"success":true,"data":{"variantId":"...","levels":[...]}}` |
| GET | `/api/v1/inventory/variants/{variantId}` | Authenticated | Alias of variant inventory summary endpoint | path: `variantId=uuid` | `{"success":true,"data":{"variantId":"...","levels":[...]}}` |
| POST | `/api/v1/inventory/warehouses` | ADMIN | Create warehouse | `{"name":"WH-BKK-01","location":"Bangkok","active":true}` | `{"success":true,"data":{"id":"...","name":"WH-BKK-01"}}` |
| GET | `/api/v1/inventory/warehouses` | Authenticated | List warehouses | none | `{"success":true,"data":[{"id":"...","name":"WH-BKK-01"}]}` |
| POST | `/api/v1/inventory/adjust` | ADMIN | Manual stock adjustment | `{"variantId":"...","warehouseId":"...","changeQuantity":20,"reason":"purchase","reorderPoint":10}` | `{"success":true,"data":{"variantId":"...","quantity":120}}` |
| POST | `/api/v1/inventory/check` | Authenticated | Check availability for multiple lines | `{"items":[{"variantId":"...","warehouseId":"...","quantity":2}]}` | `{"success":true,"data":{"items":[{"available":true,"backorder":false}]}}` |
| POST | `/api/v1/inventory/reserve` | Authenticated | Reserve stock for order or create backorder reservation when allowed | `{"variantId":"...","warehouseId":"...","orderId":"...","quantity":2,"note":"checkout"}` | `{"success":true,"data":{"id":"...","status":"RESERVED"}}` |
| POST | `/api/v1/inventory/confirm` | Authenticated | Confirm reservation after payment/shipping step | `{"reservationId":"...","note":"payment captured"}` | `{"success":true,"data":{"id":"...","status":"CONFIRMED"}}` |
| POST | `/api/v1/inventory/release` | Authenticated | Release reservation on cancellation/failure | `{"reservationId":"...","note":"payment failed"}` | `{"success":true,"data":{"id":"...","status":"RELEASED"}}` |

---

## Pricing and Promotions Endpoints

| Method | URL | Auth | Description | Request Example | Response Example |
|---|---|---|---|---|---|
| POST | `/api/v1/price-lists` | ADMIN | Create price list (`BASE`, `GROUP`, `CHANNEL`, etc.) | `{"name":"Base USD","type":"BASE"}` | `{"success":true,"data":{"id":"...","name":"Base USD","type":"BASE"}}` |
| GET | `/api/v1/price-lists` | Public | List price lists | none | `{"success":true,"data":[{"id":"...","name":"Base USD"}]}` |
| POST | `/api/v1/prices` | ADMIN | Upsert variant price entry | `{"variantId":"...","priceListId":"...","amount":89.99,"currency":"USD","priority":10}` | `{"success":true,"data":{"id":"...","variantId":"...","amount":89.99}}` |
| GET | `/api/v1/prices/variants/{variantId}` | Public | Quote computed price for variant | `?customerGroupId=uuid&channelId=uuid` | `{"success":true,"data":{"variantId":"...","baseAmount":99.99,"finalAmount":89.99,"appliedPromotionIds":[...]}}` |
| POST | `/api/v1/promotions` | ADMIN | Create promotion and applicable targets | `{"name":"SUMMER10","type":"PERCENTAGE","conditions":{"percent":10},"active":true,"applications":[{"applicableEntityType":"CATEGORY","applicableEntityId":"..."}]}` | `{"success":true,"data":{"id":"...","name":"SUMMER10"}}` |
| GET | `/api/v1/promotions/active` | Public | List active promotions | none | `{"success":true,"data":[{"id":"...","name":"SUMMER10","active":true}]}` |

---

## Store Endpoints

| Method | URL | Auth | Description | Request Example | Response Example |
|---|---|---|---|---|---|
| GET | `/api/v1/stores` | Public | List stores (active filter + pagination) | `?service=DELIVERY&openNow=true&page=0&size=20` | `{"success":true,"data":{"content":[{"id":"...","name":"Store A","openNow":true}]}}` |
| GET | `/api/v1/stores/nearest` | Public | Find nearest stores | `?lat=13.7563&lng=100.5018&limit=5` | `{"success":true,"data":[{"id":"...","distanceKm":1.45}]}` |
| PUT | `/api/v1/stores/preferred/{storeId}` | Authenticated | Set current user's preferred store | none | `{"success":true,"message":"Preferred store updated","data":null}` |
| POST | `/api/v1/stores` | ADMIN | Create store | `{"name":"Central Store","addressLine1":"123 Main","city":"Bangkok","state":"BKK","zipCode":"10000","country":"TH","region":"APAC","latitude":13.75,"longitude":100.50,"openTime":"09:00:00","closeTime":"22:00:00","active":true,"services":["PICKUP","DELIVERY"],"shippingFee":4.99,"freeShippingThreshold":50}` | `{"success":true,"data":{"id":"...","name":"Central Store"}}` |
| PUT | `/api/v1/stores/{storeId}` | ADMIN | Update store | same as create payload | `{"success":true,"message":"Store updated","data":{"id":"..."}}` |
| DELETE | `/api/v1/stores/{storeId}` | ADMIN | Delete store | none | `{"success":true,"message":"Store deleted","data":null}` |

---

## Search Endpoints

| Method | URL | Auth | Description | Request Example | Response Example |
|---|---|---|---|---|---|
| GET | `/api/v1/search/predictive` | Public | Predictive suggestions across scopes | `?q=lap&scope=all` | `{"success":true,"data":[{"value":"Laptop X","scope":"products"}]}` |
| GET | `/api/v1/search/trend-tags` | Public | Trending search tags | none | `{"success":true,"data":[{"value":"trending-now","score":93}]}` |

---

## Recommendation Endpoints

| Method | URL | Auth | Description | Request Example | Response Example |
|---|---|---|---|---|---|
| GET | `/api/v1/recommendations/mock-ai` | Public | Return AI-style recommendation payload | none | `{"success":true,"data":{"engine":"MockRanker-v1","products":[...]}}` |
| GET | `/api/v1/recommendations/personalized` | Public | Personalized recommendations | none | `{"success":true,"data":[{"id":"...","name":"Item A"}]}` |
| GET | `/api/v1/recommendations/cross-sell` | Public | Cross-sell list | none | `{"success":true,"data":[{"id":"...","name":"Item B"}]}` |
| GET | `/api/v1/recommendations/best-sellers` | Public | Best sellers | none | `{"success":true,"data":[{"id":"...","bestSeller":true}]}` |
| GET | `/api/v1/recommendations/trending` | Public | Trending products | none | `{"success":true,"data":[{"id":"...","trending":true}]}` |
| GET | `/api/v1/recommendations/deals` | Public | Price-based deal picks | none | `{"success":true,"data":[{"id":"...","price":199.99}]}` |

---

## Cart Endpoints

| Method | URL | Auth | Description | Request Example | Response Example |
|---|---|---|---|---|---|
| GET | `/api/v1/cart` | Authenticated | Get current user's cart | none | `{"success":true,"data":{"cartId":"...","items":[...],"totals":{"totalAmount":120.00}}}` |
| POST | `/api/v1/cart/items` | Authenticated | Add item to cart | `{"productId":"uuid","variantId":"uuid-or-null","quantity":2,"storeId":"uuid-or-null"}` | `{"success":true,"message":"Item added to cart","data":{"items":[...]}}` |
| PUT | `/api/v1/cart/items/{cartItemId}` | Authenticated | Update cart item quantity | `{"quantity":3}` | `{"success":true,"message":"Cart item updated","data":{"items":[...]}}` |
| DELETE | `/api/v1/cart/items/{cartItemId}` | Authenticated | Remove item from cart | none | `{"success":true,"message":"Cart item removed","data":{"items":[...]}}` |
| DELETE | `/api/v1/cart/items` | Authenticated | Clear cart | none | `{"success":true,"message":"Cart cleared","data":{"items":[]}}` |
| POST | `/api/v1/cart/coupon` | Authenticated | Apply coupon code | `{"couponCode":"SAVE10"}` | `{"success":true,"message":"Coupon applied","data":{"totals":{"couponCode":"SAVE10"}}}` |

---

## Checkout Endpoints

| Method | URL | Auth | Description | Request Example | Response Example |
|---|---|---|---|---|---|
| GET | `/api/v1/checkout/steps/review` | Authenticated | Review step preview | none | `{"success":true,"data":{"step":"review","nextStep":"shipping","cart":{...}}}` |
| POST | `/api/v1/checkout/steps/shipping` | Authenticated | Shipping step preview | none | `{"success":true,"data":{"step":"shipping","nextStep":"payment","cart":{...}}}` |
| POST | `/api/v1/checkout/steps/payment` | Authenticated | Payment step preview | none | `{"success":true,"data":{"step":"payment","nextStep":"confirm","cart":{...}}}` |
| POST | `/api/v1/checkout/steps/confirm` | Authenticated | Confirm checkout and create order | `{"fulfillmentMethod":"DELIVERY","storeId":"uuid","shippingAddressSnapshot":"123 Main","paymentReference":"PAY-123","couponCode":"SAVE10","b2bInvoice":false,"idempotencyKey":"checkout-001"}` | `{"success":true,"message":"Order confirmed","data":{"id":"...","status":"PAID","items":[...]}}` |
| POST | `/api/v1/checkout` | Authenticated | Direct checkout (same payload as confirm) | same as confirm | `{"success":true,"message":"Order confirmed","data":{"id":"...","status":"PAID"}}` |

---

## Order Endpoints

| Method | URL | Auth | Description | Request Example | Response Example |
|---|---|---|---|---|---|
| GET | `/api/v1/orders` | ADMIN | List orders (paginated) | `?page=0&size=20&sortBy=createdAt&direction=desc` | `{"success":true,"data":{"content":[{"id":"...","status":"PAID"}]}}` |
| GET | `/api/v1/orders/{orderId}` | ADMIN | Get order details | path: `orderId=uuid` | `{"success":true,"data":{"id":"...","items":[...]}}` |
| GET | `/api/v1/orders/{orderId}/timeline` | ADMIN | Get order timeline events | path: `orderId=uuid` | `{"success":true,"data":[{"status":"PAID","actor":"admin@example.com","note":"..."}]}` |
| PATCH | `/api/v1/orders/{orderId}/status` | ADMIN | Update order and refund status | `{"status":"SHIPPED","refundStatus":"NONE"}` | `{"success":true,"message":"Order status updated","data":{"id":"...","status":"SHIPPED"}}` |

---

## Notification Endpoints

| Method | URL | Auth | Description | Request Example | Response Example |
|---|---|---|---|---|---|
| GET | `/api/v1/notifications/me` | Authenticated | Get latest user notifications | none | `{"success":true,"data":[{"id":"...","category":"ORDER","read":false}]}` |
| GET | `/api/v1/notifications/me/unread-count` | Authenticated | Get unread count | none | `{"success":true,"data":4}` |
| PATCH | `/api/v1/notifications/{notificationId}/read` | Authenticated | Mark notification as read | none | `{"success":true,"data":{"id":"...","read":true}}` |
| PATCH | `/api/v1/notifications/me/read-all` | Authenticated | Mark all as read | none | `{"success":true,"data":7}` |
| POST | `/api/v1/notifications/user/{userId}` | ADMIN | Send notification to a user | `{"targetUserId":"uuid","category":"SYSTEM","title":"Maintenance","body":"Window starts at 22:00"}` | `{"success":true,"data":{"id":"...","targetUserId":"...","title":"Maintenance"}}` |
| POST | `/api/v1/notifications/broadcast` | ADMIN | Broadcast notification | `{"targetUserId":null,"category":"SYSTEM","title":"Announcement","body":"Platform update complete"}` | `{"success":true,"message":"Broadcast sent","data":null}` |

---

## Account Endpoints

| Method | URL | Auth | Description | Request Example | Response Example |
|---|---|---|---|---|---|
| GET | `/api/v1/account/profile` | Authenticated | Get current user profile | none | `{"success":true,"data":{"id":"...","email":"user@example.com","roles":["CUSTOMER"]}}` |
| PUT | `/api/v1/account/profile` | Authenticated | Update profile | `{"fullName":"Jane Doe","phone":"+1-555-0101"}` | `{"success":true,"message":"Profile updated","data":{"fullName":"Jane Doe"}}` |
| DELETE | `/api/v1/account/profile` | Authenticated | Delete account | none | `{"success":true,"message":"Account deleted","data":null}` |
| GET | `/api/v1/account/addresses` | Authenticated | List addresses | none | `{"success":true,"data":[{"id":"...","line1":"123 Main"}]}` |
| POST | `/api/v1/account/addresses` | Authenticated | Add address | `{"label":"Home","fullName":"Jane Doe","line1":"123 Main","city":"NYC","state":"NY","zipCode":"10001","country":"US","defaultAddress":true}` | `{"success":true,"message":"Address added","data":{"id":"..."}}` |
| PUT | `/api/v1/account/addresses/{addressId}` | Authenticated | Update address | same as add address payload | `{"success":true,"message":"Address updated","data":{"id":"..."}}` |
| DELETE | `/api/v1/account/addresses/{addressId}` | Authenticated | Delete address | none | `{"success":true,"message":"Address deleted","data":null}` |
| GET | `/api/v1/account/payment-methods` | Authenticated | List payment methods | none | `{"success":true,"data":[{"id":"...","methodType":"CARD","provider":"VISA"}]}` |
| POST | `/api/v1/account/payment-methods` | Authenticated | Add payment method | `{"methodType":"CARD","provider":"VISA","tokenizedReference":"tok_abc","defaultMethod":true}` | `{"success":true,"message":"Payment method added","data":{"id":"..."}}` |
| PUT | `/api/v1/account/payment-methods/{paymentMethodId}` | Authenticated | Update payment method | same as add payment method payload | `{"success":true,"message":"Payment method updated","data":{"id":"..."}}` |
| DELETE | `/api/v1/account/payment-methods/{paymentMethodId}` | Authenticated | Delete payment method | none | `{"success":true,"message":"Payment method deleted","data":null}` |
| GET | `/api/v1/account/orders` | Authenticated | Get my recent order history | none | `{"success":true,"data":[{"id":"...","status":"DELIVERED"}]}` |
| POST | `/api/v1/account/orders/{orderId}/quick-reorder` | Authenticated | Copy old order items into cart and return history | none | `{"success":true,"message":"Quick reorder prepared","data":[{"id":"..."}]}` |
| PUT | `/api/v1/account/company-profile` | ADMIN or B2B | Upsert B2B company profile | `{"companyName":"Acme Corp","taxId":"TAX-123","costCenter":"CC-01","approvalEmail":"manager@acme.com","approvalRequired":true,"approvalThreshold":1000}` | `{"success":true,"message":"Company profile saved","data":{"id":"...","approvalRequired":true}}` |
| GET | `/api/v1/account/approvals` | Authenticated | Get my approval requests | none | `{"success":true,"data":[{"id":"...","status":"PENDING","amount":1500}]}` |

---

## Admin Endpoints

| Method | URL | Auth | Description | Request Example | Response Example |
|---|---|---|---|---|---|
| GET | `/api/v1/admin/dashboard/summary` | ADMIN | Dashboard summary metrics | none | `{"success":true,"data":{"revenue":25000,"ordersCount":120,"usersCount":90,"storesCount":8}}` |
| GET | `/api/v1/admin/b2b/approvals` | ADMIN | List pending B2B approvals | none | `{"success":true,"data":[{"id":"...","status":"PENDING"}]}` |
| PATCH | `/api/v1/admin/b2b/approvals/{approvalId}` | ADMIN | Update approval status | `{"status":"APPROVED","reviewerNotes":"Approved by finance"}` | `{"success":true,"message":"Approval updated","data":{"id":"...","status":"APPROVED"}}` |
| GET | `/api/v1/admin/users` | ADMIN | List users (paginated) | `?page=0&size=20&sortBy=createdAt&direction=desc` | `{"success":true,"data":{"content":[{"id":"...","roles":["CUSTOMER"],"enabled":true}]}}` |
| PATCH | `/api/v1/admin/users/{userId}` | ADMIN | Update user roles and/or enabled state | `{"roles":["ADMIN","CUSTOMER"],"enabled":true}` | `{"success":true,"message":"User updated","data":{"id":"...","roles":["ADMIN","CUSTOMER"]}}` |

---

## WebSocket / Realtime (Non-REST)

- STOMP/SockJS endpoint: `/ws`
- Topic broadcast:
  - `/topic/notifications`
  - `/topic/notifications/{targetUserId}`
- User destination:
  - `/user/queue/notifications`

---

## Common Error Codes Seen in Current Implementation

- `AUTH_REQUIRED`
- `ACCESS_DENIED`
- `AUTH_INVALID`
- `REFRESH_INVALID`
- `REFRESH_EXPIRED`
- `RESET_INVALID`
- `RESET_EXPIRED`
- `USER_NOT_FOUND`
- `PRODUCT_NOT_FOUND`
- `STORE_NOT_FOUND`
- `STOCK_INSUFFICIENT`
- `COUPON_INVALID`
- `COUPON_NOT_ACTIVE`
- `COUPON_EXPIRED`
- `COUPON_MULTI_NOT_SUPPORTED`
- `ORDER_STATUS_INVALID`
- `VALIDATION_ERROR`
- `INTERNAL_ERROR`

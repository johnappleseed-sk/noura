abstract final class ApiEndpoints {
  // Runtime
  static const runtimeFeatures = '/runtime/features';

  // Auth
  static const authRegister = '/auth/register';
  static const authLogin = '/auth/login';
  static const authRefresh = '/auth/refresh';
  static const authPasswordResetRequest = '/auth/password-reset/request';
  static const authPasswordResetConfirm = '/auth/password-reset/confirm';

  // Account
  static const accountProfile = '/account/profile';
  static const accountAddresses = '/account/addresses';
  static String accountAddressById(String addressId) =>
      '/account/addresses/$addressId';
  static String accountAddressSetDefault(String addressId) =>
      '/account/addresses/$addressId/set-default';
  static const accountPaymentMethods = '/account/payment-methods';
  static String accountPaymentMethodById(String paymentMethodId) =>
      '/account/payment-methods/$paymentMethodId';
  static const accountOrders = '/account/orders';
  static String accountOrderQuickReorder(String orderId) =>
      '/account/orders/$orderId/quick-reorder';

  // Catalog
  static const categoriesTree = '/categories/tree';
  static const products = '/products';
  static String productById(String productId) => '/products/$productId';
  static String productInventory(String productId) =>
      '/products/$productId/inventory';
  static String productReviews(String productId) =>
      '/products/$productId/reviews';
  static String relatedProducts(String productId) =>
      '/products/$productId/related';
  static String frequentlyBoughtTogether(String productId) =>
      '/products/$productId/frequently-bought-together';

  // Merchandising & search
  static const merchandisingProducts = '/merchandising/products';
  static const predictiveSearch = '/search/predictive';
  static const trendTags = '/search/trend-tags';

  // Recommendations
  static const recommendationsTrending = '/recommendations/trending';
  static const recommendationsBestSellers = '/recommendations/best-sellers';
  static const recommendationsDeals = '/recommendations/deals';
  static const recommendationsPersonalized = '/recommendations/personalized';
  static const recommendationsCrossSell = '/recommendations/cross-sell';

  // Cart / checkout / orders
  static const cart = '/cart';
  static const cartItems = '/cart/items';
  static String cartItemById(String cartItemId) => '/cart/items/$cartItemId';
  static const cartCoupon = '/cart/coupon';
  static const checkout = '/checkout';
  static const checkoutReviewStep = '/checkout/steps/review';
  static const checkoutShippingStep = '/checkout/steps/shipping';
  static const checkoutPaymentStep = '/checkout/steps/payment';
  static const checkoutConfirmStep = '/checkout/steps/confirm';
  static const orders = '/orders';
  static String orderById(String orderId) => '/orders/$orderId';
  static String orderStatus(String orderId) => '/orders/$orderId/status';
  static String orderTimeline(String orderId) => '/orders/$orderId/timeline';

  // Discovery
  static const heroCarousel = '/carousels/hero';
  static const promotionsActive = '/promotions/active';
  static const stores = '/stores';
  static const storesNearest = '/stores/nearest';
  static String storesPreferred(String storeId) => '/stores/preferred/$storeId';
  static String storeById(String storeId) => '/stores/$storeId';

  // Location
  static const locationResolve = '/location/resolve';
  static const locationForwardGeocode = '/location/forward-geocode';
  static const locationReverseGeocode = '/location/reverse-geocode';
  static const locationValidateServiceArea = '/location/validate-service-area';
  static const locationNearbyStores = '/location/nearby-stores';

  // Notifications
  static const notificationsMe = '/notifications/me';
  static const notificationsUnreadCount = '/notifications/me/unread-count';
  static String notificationRead(String notificationId) =>
      '/notifications/$notificationId/read';
  static const notificationsReadAll = '/notifications/me/read-all';
}

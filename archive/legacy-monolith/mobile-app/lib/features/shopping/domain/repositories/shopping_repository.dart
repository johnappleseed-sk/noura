import '../entities/address.dart';
import '../entities/cart.dart';
import '../entities/checkout_preview.dart';
import '../entities/order.dart';
import '../entities/payment_method.dart';

abstract class ShoppingRepository {
  Future<Cart> getCart();

  Future<Cart> addCartItem({
    required String productId,
    String? variantId,
    int quantity = 1,
    String? storeId,
    String? analyticsListName,
    int? analyticsSlot,
    String? analyticsPagePath,
  });

  Future<Cart> updateCartItem({
    required String cartItemId,
    required int quantity,
  });

  Future<Cart> removeCartItem({required String cartItemId});

  Future<Cart> clearCartItems();

  Future<Cart> applyCoupon({required String couponCode});

  Future<List<Address>> getAddresses();

  Future<Address> addAddress({
    required String fullName,
    required String line1,
    required String city,
    required String state,
    required String zipCode,
    required String country,
    String? label,
    String? phone,
    String? line2,
    String? district,
    String? deliveryInstructions,
    bool defaultAddress = false,
  });

  Future<Address> updateAddress({
    required String addressId,
    required String fullName,
    required String line1,
    required String city,
    required String state,
    required String zipCode,
    required String country,
    String? label,
    String? phone,
    String? line2,
    String? district,
    String? deliveryInstructions,
    bool defaultAddress = false,
  });

  Future<void> deleteAddress({required String addressId});

  Future<void> setDefaultAddress({required String addressId});

  Future<List<PaymentMethod>> getPaymentMethods();

  Future<PaymentMethod> addPaymentMethod({
    required String methodType,
    required String provider,
    required String tokenizedReference,
    bool defaultMethod = false,
  });

  Future<PaymentMethod> updatePaymentMethod({
    required String paymentMethodId,
    required String methodType,
    required String provider,
    required String tokenizedReference,
    bool defaultMethod = false,
  });

  Future<void> deletePaymentMethod({required String paymentMethodId});

  Future<CheckoutPreview> reviewCheckoutStep();

  Future<CheckoutPreview> submitShippingStep({
    required String fulfillmentMethod,
    String? storeId,
    String? addressId,
    String? shippingAddressSnapshot,
  });

  Future<CheckoutPreview> submitPaymentStep({
    String? paymentReference,
    String? couponCode,
    bool? b2bInvoice,
    String? idempotencyKey,
  });

  Future<Order> confirmCheckout({
    String? fulfillmentMethod,
    String? storeId,
    String? addressId,
    String? shippingAddressSnapshot,
    String? paymentReference,
    String? couponCode,
    bool? b2bInvoice,
    String? idempotencyKey,
  });

  Future<Order> getOrderById({required String orderId});

  Future<List<OrderTimelineEvent>> getOrderTimeline({required String orderId});
}

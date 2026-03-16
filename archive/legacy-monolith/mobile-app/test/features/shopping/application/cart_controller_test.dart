import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:noura/features/shopping/application/cart_controller.dart';
import 'package:noura/features/shopping/data/repositories/shopping_repository_impl.dart';
import 'package:noura/features/shopping/domain/entities/address.dart';
import 'package:noura/features/shopping/domain/entities/cart.dart';
import 'package:noura/features/shopping/domain/entities/cart_item.dart';
import 'package:noura/features/shopping/domain/entities/cart_totals.dart';
import 'package:noura/features/shopping/domain/entities/checkout_preview.dart';
import 'package:noura/features/shopping/domain/entities/order.dart';
import 'package:noura/features/shopping/domain/entities/payment_method.dart';
import 'package:noura/features/shopping/domain/repositories/shopping_repository.dart';

class FakeShoppingRepository implements ShoppingRepository {
  static const _cart = Cart(
    cartId: 'cart-1',
    storeId: 'store-1',
    addressId: '',
    items: <CartItem>[],
    totals: CartTotals(
      subtotal: 0,
      discountAmount: 0,
      shippingAmount: 0,
      totalAmount: 0,
      couponCode: '',
      appliedPromotionCodes: <String>[],
      freeShippingApplied: false,
    ),
  );

  @override
  Future<Cart> getCart() async => _cart;

  @override
  Future<Cart> addCartItem({
    required String productId,
    String? variantId,
    int quantity = 1,
    String? storeId,
    String? analyticsListName,
    int? analyticsSlot,
    String? analyticsPagePath,
  }) async {
    return _cart;
  }

  @override
  Future<Cart> updateCartItem({
    required String cartItemId,
    required int quantity,
  }) async {
    return _cart;
  }

  @override
  Future<Cart> removeCartItem({required String cartItemId}) async => _cart;

  @override
  Future<Cart> clearCartItems() async => _cart;

  @override
  Future<Cart> applyCoupon({required String couponCode}) async => _cart;

  @override
  Future<List<Address>> getAddresses() async => const <Address>[];

  @override
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
  }) async {
    throw UnimplementedError();
  }

  @override
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
  }) async {
    throw UnimplementedError();
  }

  @override
  Future<void> deleteAddress({required String addressId}) async {}

  @override
  Future<void> setDefaultAddress({required String addressId}) async {}

  @override
  Future<List<PaymentMethod>> getPaymentMethods() async {
    return const <PaymentMethod>[];
  }

  @override
  Future<PaymentMethod> addPaymentMethod({
    required String methodType,
    required String provider,
    required String tokenizedReference,
    bool defaultMethod = false,
  }) async {
    throw UnimplementedError();
  }

  @override
  Future<PaymentMethod> updatePaymentMethod({
    required String paymentMethodId,
    required String methodType,
    required String provider,
    required String tokenizedReference,
    bool defaultMethod = false,
  }) async {
    throw UnimplementedError();
  }

  @override
  Future<void> deletePaymentMethod({required String paymentMethodId}) async {}

  @override
  Future<CheckoutPreview> reviewCheckoutStep() async {
    throw UnimplementedError();
  }

  @override
  Future<CheckoutPreview> submitShippingStep({
    required String fulfillmentMethod,
    String? storeId,
    String? addressId,
    String? shippingAddressSnapshot,
  }) async {
    throw UnimplementedError();
  }

  @override
  Future<CheckoutPreview> submitPaymentStep({
    String? paymentReference,
    String? couponCode,
    bool? b2bInvoice,
    String? idempotencyKey,
  }) async {
    throw UnimplementedError();
  }

  @override
  Future<Order> confirmCheckout({
    String? fulfillmentMethod,
    String? storeId,
    String? addressId,
    String? shippingAddressSnapshot,
    String? paymentReference,
    String? couponCode,
    bool? b2bInvoice,
    String? idempotencyKey,
  }) async {
    throw UnimplementedError();
  }

  @override
  Future<Order> getOrderById({required String orderId}) async {
    throw UnimplementedError();
  }

  @override
  Future<List<OrderTimelineEvent>> getOrderTimeline({
    required String orderId,
  }) async {
    return const <OrderTimelineEvent>[];
  }
}

void main() {
  test('CartController validates coupon and quantity inputs', () async {
    final container = ProviderContainer(
      overrides: [
        shoppingRepositoryProvider.overrideWithValue(FakeShoppingRepository()),
      ],
    );
    addTearDown(container.dispose);

    await container.read(cartControllerProvider.future);

    final applyCouponOk = await container
        .read(cartControllerProvider.notifier)
        .applyCoupon('   ');
    expect(applyCouponOk, isFalse);
    expect(
      container.read(cartControllerProvider).valueOrNull?.actionError,
      'Please enter a coupon code.',
    );

    final addItemOk = await container
        .read(cartControllerProvider.notifier)
        .addItem(productId: 'p-1', quantity: 0);
    expect(addItemOk, isFalse);
    expect(
      container.read(cartControllerProvider).valueOrNull?.actionError,
      'Quantity must be at least 1.',
    );
  });
}

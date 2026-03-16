import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:noura/features/shopping/application/checkout_controller.dart';
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
  Future<Cart> removeCartItem({required String cartItemId}) async {
    return _cart;
  }

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
    return const <PaymentMethod>[
      PaymentMethod(
        id: 'pm-1',
        methodType: 'CARD',
        provider: 'DemoPay',
        tokenizedReference: 'tok_1234',
        defaultMethod: true,
      ),
    ];
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
    return CheckoutPreview(
      step: 'review',
      nextStep: 'shipping',
      message: 'Ready',
      cart: _cart,
    );
  }

  @override
  Future<CheckoutPreview> submitShippingStep({
    required String fulfillmentMethod,
    String? storeId,
    String? addressId,
    String? shippingAddressSnapshot,
  }) async {
    return CheckoutPreview(
      step: 'shipping',
      nextStep: 'payment',
      message: 'Shipping saved',
      cart: _cart,
    );
  }

  @override
  Future<CheckoutPreview> submitPaymentStep({
    String? paymentReference,
    String? couponCode,
    bool? b2bInvoice,
    String? idempotencyKey,
  }) async {
    return CheckoutPreview(
      step: 'payment',
      nextStep: 'confirm',
      message: 'Payment saved',
      cart: _cart,
    );
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
    return Order(
      id: 'order-1',
      userId: 'user-1',
      storeId: 'store-1',
      subtotal: 25,
      discountAmount: 0,
      shippingAmount: 2.5,
      totalAmount: 27.5,
      fulfillmentMethod: fulfillmentMethod ?? 'DELIVERY',
      status: 'CREATED',
      refundStatus: 'NONE',
      couponCode: couponCode ?? '',
      createdAt: DateTime.parse('2026-03-11T10:00:00Z'),
      items: const <OrderItem>[],
    );
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

  static const _cart = Cart(
    cartId: 'cart-1',
    storeId: 'store-1',
    addressId: 'address-1',
    items: <CartItem>[
      CartItem(
        id: 'item-1',
        productId: 'product-1',
        productName: 'Product',
        quantity: 1,
        unitPrice: 25,
        lineTotal: 25,
      ),
    ],
    totals: CartTotals(
      subtotal: 25,
      discountAmount: 0,
      shippingAmount: 2.5,
      totalAmount: 27.5,
      couponCode: '',
      appliedPromotionCodes: <String>[],
      freeShippingApplied: false,
    ),
  );
}

void main() {
  test('CheckoutController submits steps and confirms order', () async {
    final container = ProviderContainer(
      overrides: [
        shoppingRepositoryProvider.overrideWithValue(FakeShoppingRepository()),
      ],
    );
    addTearDown(container.dispose);

    final initial = await container.read(checkoutControllerProvider.future);
    expect(initial.preview.step, 'review');

    container
        .read(checkoutControllerProvider.notifier)
        .setFulfillmentMethod('DELIVERY');
    container
        .read(checkoutControllerProvider.notifier)
        .setSelectedAddress('address-1');
    container
        .read(checkoutControllerProvider.notifier)
        .setPaymentReference('tok_1234');

    final shippingOk = await container
        .read(checkoutControllerProvider.notifier)
        .submitShippingStep();
    expect(shippingOk, isTrue);

    final paymentOk = await container
        .read(checkoutControllerProvider.notifier)
        .submitPaymentStep();
    expect(paymentOk, isTrue);

    final confirmOk = await container
        .read(checkoutControllerProvider.notifier)
        .confirmOrder();
    expect(confirmOk, isTrue);

    final state = container.read(checkoutControllerProvider).valueOrNull;
    expect(state?.placedOrder?.id, 'order-1');
  });
}

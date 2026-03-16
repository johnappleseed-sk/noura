import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client.dart';
import '../../../../core/network/api_endpoints.dart';
import '../../../../core/providers/core_providers.dart';
import '../../domain/entities/address.dart';
import '../../domain/entities/cart.dart';
import '../../domain/entities/checkout_preview.dart';
import '../../domain/entities/order.dart';
import '../../domain/entities/payment_method.dart';
import '../../domain/repositories/shopping_repository.dart';
import '../dto/add_cart_item_request_dto.dart';
import '../dto/address_request_dto.dart';
import '../dto/apply_coupon_request_dto.dart';
import '../dto/checkout_confirm_request_dto.dart';
import '../dto/checkout_payment_request_dto.dart';
import '../dto/checkout_shipping_request_dto.dart';
import '../dto/payment_method_request_dto.dart';
import '../dto/update_cart_item_request_dto.dart';

class ShoppingRepositoryImpl implements ShoppingRepository {
  ShoppingRepositoryImpl({required ApiClient apiClient})
    : _apiClient = apiClient;

  final ApiClient _apiClient;

  @override
  Future<Cart> getCart() {
    return _apiClient.get<Cart>(
      ApiEndpoints.cart,
      parser: (Object? value) => Cart.fromJson(_parseMap(value)),
    );
  }

  @override
  Future<Cart> addCartItem({
    required String productId,
    String? variantId,
    int quantity = 1,
    String? storeId,
    String? analyticsListName,
    int? analyticsSlot,
    String? analyticsPagePath,
  }) {
    final requestDto = AddCartItemRequestDto(
      productId: productId,
      variantId: variantId,
      quantity: quantity,
      storeId: storeId,
      analyticsListName: analyticsListName,
      analyticsSlot: analyticsSlot,
      analyticsPagePath: analyticsPagePath,
    );
    return _apiClient.post<Cart>(
      ApiEndpoints.cartItems,
      body: requestDto.toJson(),
      parser: (Object? value) => Cart.fromJson(_parseMap(value)),
    );
  }

  @override
  Future<Cart> updateCartItem({
    required String cartItemId,
    required int quantity,
  }) {
    final requestDto = UpdateCartItemRequestDto(quantity: quantity);
    return _apiClient.put<Cart>(
      ApiEndpoints.cartItemById(cartItemId),
      body: requestDto.toJson(),
      parser: (Object? value) => Cart.fromJson(_parseMap(value)),
    );
  }

  @override
  Future<Cart> removeCartItem({required String cartItemId}) {
    return _apiClient.delete<Cart>(
      ApiEndpoints.cartItemById(cartItemId),
      parser: (Object? value) => Cart.fromJson(_parseMap(value)),
    );
  }

  @override
  Future<Cart> clearCartItems() {
    return _apiClient.delete<Cart>(
      ApiEndpoints.cartItems,
      parser: (Object? value) => Cart.fromJson(_parseMap(value)),
    );
  }

  @override
  Future<Cart> applyCoupon({required String couponCode}) {
    final requestDto = ApplyCouponRequestDto(couponCode: couponCode);
    return _apiClient.post<Cart>(
      ApiEndpoints.cartCoupon,
      body: requestDto.toJson(),
      parser: (Object? value) => Cart.fromJson(_parseMap(value)),
    );
  }

  @override
  Future<List<Address>> getAddresses() {
    return _apiClient.get<List<Address>>(
      ApiEndpoints.accountAddresses,
      parser: (Object? value) =>
          _parseList(value).map(Address.fromJson).toList(growable: false),
    );
  }

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
  }) {
    final requestDto = AddressRequestDto(
      fullName: fullName,
      line1: line1,
      city: city,
      state: state,
      zipCode: zipCode,
      country: country,
      label: label,
      phone: phone,
      line2: line2,
      district: district,
      deliveryInstructions: deliveryInstructions,
      defaultAddress: defaultAddress,
    );

    return _apiClient.post<Address>(
      ApiEndpoints.accountAddresses,
      body: requestDto.toJson(),
      parser: (Object? value) => Address.fromJson(_parseMap(value)),
    );
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
  }) {
    final requestDto = AddressRequestDto(
      fullName: fullName,
      line1: line1,
      city: city,
      state: state,
      zipCode: zipCode,
      country: country,
      label: label,
      phone: phone,
      line2: line2,
      district: district,
      deliveryInstructions: deliveryInstructions,
      defaultAddress: defaultAddress,
    );

    return _apiClient.put<Address>(
      ApiEndpoints.accountAddressById(addressId),
      body: requestDto.toJson(),
      parser: (Object? value) => Address.fromJson(_parseMap(value)),
    );
  }

  @override
  Future<void> deleteAddress({required String addressId}) {
    return _apiClient.delete<void>(
      ApiEndpoints.accountAddressById(addressId),
      parser: (_) {},
    );
  }

  @override
  Future<void> setDefaultAddress({required String addressId}) {
    return _apiClient.post<void>(
      ApiEndpoints.accountAddressSetDefault(addressId),
      parser: (_) {},
    );
  }

  @override
  Future<List<PaymentMethod>> getPaymentMethods() {
    return _apiClient.get<List<PaymentMethod>>(
      ApiEndpoints.accountPaymentMethods,
      parser: (Object? value) =>
          _parseList(value).map(PaymentMethod.fromJson).toList(growable: false),
    );
  }

  @override
  Future<PaymentMethod> addPaymentMethod({
    required String methodType,
    required String provider,
    required String tokenizedReference,
    bool defaultMethod = false,
  }) {
    final requestDto = PaymentMethodRequestDto(
      methodType: methodType,
      provider: provider,
      tokenizedReference: tokenizedReference,
      defaultMethod: defaultMethod,
    );
    return _apiClient.post<PaymentMethod>(
      ApiEndpoints.accountPaymentMethods,
      body: requestDto.toJson(),
      parser: (Object? value) => PaymentMethod.fromJson(_parseMap(value)),
    );
  }

  @override
  Future<PaymentMethod> updatePaymentMethod({
    required String paymentMethodId,
    required String methodType,
    required String provider,
    required String tokenizedReference,
    bool defaultMethod = false,
  }) {
    final requestDto = PaymentMethodRequestDto(
      methodType: methodType,
      provider: provider,
      tokenizedReference: tokenizedReference,
      defaultMethod: defaultMethod,
    );
    return _apiClient.put<PaymentMethod>(
      ApiEndpoints.accountPaymentMethodById(paymentMethodId),
      body: requestDto.toJson(),
      parser: (Object? value) => PaymentMethod.fromJson(_parseMap(value)),
    );
  }

  @override
  Future<void> deletePaymentMethod({required String paymentMethodId}) {
    return _apiClient.delete<void>(
      ApiEndpoints.accountPaymentMethodById(paymentMethodId),
      parser: (_) {},
    );
  }

  @override
  Future<CheckoutPreview> reviewCheckoutStep() {
    return _apiClient.get<CheckoutPreview>(
      ApiEndpoints.checkoutReviewStep,
      parser: (Object? value) => CheckoutPreview.fromJson(_parseMap(value)),
    );
  }

  @override
  Future<CheckoutPreview> submitShippingStep({
    required String fulfillmentMethod,
    String? storeId,
    String? addressId,
    String? shippingAddressSnapshot,
  }) {
    final requestDto = CheckoutShippingRequestDto(
      fulfillmentMethod: fulfillmentMethod,
      storeId: storeId,
      addressId: addressId,
      shippingAddressSnapshot: shippingAddressSnapshot,
    );
    return _apiClient.post<CheckoutPreview>(
      ApiEndpoints.checkoutShippingStep,
      body: requestDto.toJson(),
      parser: (Object? value) => CheckoutPreview.fromJson(_parseMap(value)),
    );
  }

  @override
  Future<CheckoutPreview> submitPaymentStep({
    String? paymentReference,
    String? couponCode,
    bool? b2bInvoice,
    String? idempotencyKey,
  }) {
    final requestDto = CheckoutPaymentRequestDto(
      paymentReference: paymentReference,
      couponCode: couponCode,
      b2bInvoice: b2bInvoice,
      idempotencyKey: idempotencyKey,
    );
    return _apiClient.post<CheckoutPreview>(
      ApiEndpoints.checkoutPaymentStep,
      body: requestDto.toJson(),
      parser: (Object? value) => CheckoutPreview.fromJson(_parseMap(value)),
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
  }) {
    final requestDto = CheckoutConfirmRequestDto(
      fulfillmentMethod: fulfillmentMethod,
      storeId: storeId,
      addressId: addressId,
      shippingAddressSnapshot: shippingAddressSnapshot,
      paymentReference: paymentReference,
      couponCode: couponCode,
      b2bInvoice: b2bInvoice,
      idempotencyKey: idempotencyKey,
    );
    return _apiClient.post<Order>(
      ApiEndpoints.checkoutConfirmStep,
      body: requestDto.toJson(),
      parser: (Object? value) => Order.fromJson(_parseMap(value)),
    );
  }

  @override
  Future<Order> getOrderById({required String orderId}) {
    return _apiClient.get<Order>(
      ApiEndpoints.orderById(orderId),
      parser: (Object? value) => Order.fromJson(_parseMap(value)),
    );
  }

  @override
  Future<List<OrderTimelineEvent>> getOrderTimeline({required String orderId}) {
    return _apiClient.get<List<OrderTimelineEvent>>(
      ApiEndpoints.orderTimeline(orderId),
      parser: (Object? value) => _parseList(
        value,
      ).map(OrderTimelineEvent.fromJson).toList(growable: false),
    );
  }

  Map<String, dynamic> _parseMap(Object? value) {
    if (value is Map<String, dynamic>) {
      return value;
    }
    if (value is Map) {
      return value.map<String, dynamic>(
        (Object? key, Object? mapValue) => MapEntry(key.toString(), mapValue),
      );
    }
    return const <String, dynamic>{};
  }

  List<Map<String, dynamic>> _parseList(Object? value) {
    if (value is List<dynamic>) {
      return value.whereType<Map<String, dynamic>>().toList(growable: false);
    }
    return const <Map<String, dynamic>>[];
  }
}

final shoppingRepositoryProvider = Provider<ShoppingRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return ShoppingRepositoryImpl(apiClient: apiClient);
});

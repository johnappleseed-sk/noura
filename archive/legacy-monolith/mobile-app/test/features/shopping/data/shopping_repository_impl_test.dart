import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:noura/core/network/api_client.dart';
import 'package:noura/core/network/api_endpoints.dart';
import 'package:noura/features/shopping/data/repositories/shopping_repository_impl.dart';

class FakeApiClient extends ApiClient {
  FakeApiClient() : super(Dio());

  String? lastPath;
  Object? lastBody;
  Object? nextResponse;

  @override
  Future<T> get<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    JsonParser<T>? parser,
    bool requiresAuth = true,
    bool retryable = true,
    CancelToken? cancelToken,
  }) async {
    lastPath = path;
    if (parser != null) {
      final dynamic parsed = parser(nextResponse);
      return parsed as T;
    }
    return nextResponse as T;
  }

  @override
  Future<T> post<T>(
    String path, {
    Object? body,
    Map<String, dynamic>? queryParameters,
    JsonParser<T>? parser,
    bool requiresAuth = true,
    bool retryable = false,
    CancelToken? cancelToken,
  }) async {
    lastPath = path;
    lastBody = body;
    if (parser != null) {
      final dynamic parsed = parser(nextResponse);
      return parsed as T;
    }
    return nextResponse as T;
  }
}

void main() {
  group('ShoppingRepositoryImpl', () {
    late FakeApiClient apiClient;
    late ShoppingRepositoryImpl repository;

    setUp(() {
      apiClient = FakeApiClient();
      repository = ShoppingRepositoryImpl(apiClient: apiClient);
    });

    test('getCart maps cart payload', () async {
      apiClient.nextResponse = <String, dynamic>{
        'cartId': 'cart-1',
        'storeId': 'store-1',
        'addressId': 'address-1',
        'items': <Map<String, dynamic>>[
          <String, dynamic>{
            'id': 'item-1',
            'productId': 'product-1',
            'productName': 'Product 1',
            'quantity': 2,
            'unitPrice': 12.5,
            'lineTotal': 25.0,
          },
        ],
        'totals': <String, dynamic>{
          'subtotal': 25.0,
          'discountAmount': 0,
          'shippingAmount': 2.5,
          'totalAmount': 27.5,
          'couponCode': '',
          'appliedPromotionCodes': <String>[],
          'freeShippingApplied': false,
        },
      };

      final cart = await repository.getCart();

      expect(apiClient.lastPath, ApiEndpoints.cart);
      expect(cart.cartId, 'cart-1');
      expect(cart.items, hasLength(1));
      expect(cart.items.first.productName, 'Product 1');
      expect(cart.totals.totalAmount, 27.5);
    });

    test('applyCoupon sends coupon request body', () async {
      apiClient.nextResponse = <String, dynamic>{
        'cartId': 'cart-1',
        'storeId': 'store-1',
        'addressId': 'address-1',
        'items': <Map<String, dynamic>>[],
        'totals': <String, dynamic>{
          'subtotal': 0,
          'discountAmount': 0,
          'shippingAmount': 0,
          'totalAmount': 0,
          'couponCode': 'SPRING26',
          'appliedPromotionCodes': <String>['PROMO'],
          'freeShippingApplied': false,
        },
      };

      final cart = await repository.applyCoupon(couponCode: 'SPRING26');

      expect(apiClient.lastPath, ApiEndpoints.cartCoupon);
      expect(apiClient.lastBody, <String, dynamic>{'couponCode': 'SPRING26'});
      expect(cart.totals.couponCode, 'SPRING26');
    });
  });
}

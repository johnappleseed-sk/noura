import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:noura/core/network/api_client.dart';
import 'package:noura/core/network/api_endpoints.dart';
import 'package:noura/features/commerce/data/repositories/commerce_repository_impl.dart';

class FakeApiClient extends ApiClient {
  FakeApiClient() : super(Dio());

  String? lastPath;
  Map<String, dynamic>? lastQueryParameters;
  Object? lastBody;
  bool? lastRequiresAuth;
  Object? nextResponse;
  Object? nextPostResponse;

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
    lastQueryParameters = queryParameters;
    lastRequiresAuth = requiresAuth;
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
    lastQueryParameters = queryParameters;
    lastRequiresAuth = requiresAuth;
    if (parser != null) {
      final dynamic parsed = parser(nextPostResponse);
      return parsed as T;
    }
    return nextPostResponse as T;
  }
}

void main() {
  group('CommerceRepositoryImpl', () {
    late FakeApiClient apiClient;
    late CommerceRepositoryImpl repository;

    setUp(() {
      apiClient = FakeApiClient();
      repository = CommerceRepositoryImpl(apiClient: apiClient);
    });

    test('getProducts maps merchandising page response', () async {
      apiClient.nextResponse = <String, dynamic>{
        'content': <Map<String, dynamic>>[
          <String, dynamic>{
            'id': 'p-1',
            'name': 'Sample Product',
            'categoryId': 'c-1',
            'categoryName': 'Category',
            'price': 25.5,
            'compareAtPrice': 35.0,
            'imageUrl': 'https://cdn.example.com/p-1.jpg',
            'stockQty': 5,
            'lowStock': true,
            'allowNegativeStock': false,
            'isNew': true,
            'isTrending': false,
            'isBestseller': true,
            'merchandisingScore': 11.1,
          },
        ],
        'page': 0,
        'size': 12,
        'totalElements': 1,
        'totalPages': 1,
        'first': true,
        'last': true,
      };

      final result = await repository.getProducts(
        query: 'sample',
        categoryId: 'c-1',
        sort: 'featured',
        page: 0,
        size: 12,
      );

      expect(apiClient.lastPath, ApiEndpoints.merchandisingProducts);
      expect(apiClient.lastRequiresAuth, isFalse);
      expect(apiClient.lastQueryParameters?['query'], 'sample');
      expect(result.items, hasLength(1));
      expect(result.items.first.name, 'Sample Product');
      expect(result.hasNext, isFalse);
    });

    test('addProductReview sends payload and maps response', () async {
      apiClient.nextPostResponse = <String, dynamic>{
        'id': 'r-1',
        'userId': 'u-1',
        'userName': 'Jane',
        'rating': 5,
        'comment': 'Great product',
      };

      final review = await repository.addProductReview(
        productId: 'p-1',
        rating: 5,
        comment: 'Great product',
      );

      expect(apiClient.lastPath, ApiEndpoints.productReviews('p-1'));
      expect(apiClient.lastRequiresAuth, isTrue);
      expect(apiClient.lastBody, <String, dynamic>{
        'rating': 5,
        'comment': 'Great product',
      });
      expect(review.userName, 'Jane');
      expect(review.rating, 5);
    });
  });
}

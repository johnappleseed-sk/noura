import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client.dart';
import '../../../../core/network/api_endpoints.dart';
import '../../../../core/providers/core_providers.dart';
import '../dto/create_product_review_request_dto.dart';
import '../../domain/entities/category_node.dart';
import '../../domain/entities/hero_slide.dart';
import '../../domain/entities/paged_result.dart';
import '../../domain/entities/product_card.dart';
import '../../domain/entities/product_detail.dart';
import '../../domain/entities/product_review.dart';
import '../../domain/entities/recommendation_product.dart';
import '../../domain/entities/search_suggestion.dart';
import '../../domain/entities/trend_tag.dart';
import '../../domain/repositories/commerce_repository.dart';

class CommerceRepositoryImpl implements CommerceRepository {
  CommerceRepositoryImpl({required ApiClient apiClient})
    : _apiClient = apiClient;

  final ApiClient _apiClient;

  @override
  Future<List<HeroSlide>> getHeroSlides({
    String? storeId,
    String? channelId,
    String? locale,
    String? audienceSegment,
    String? previewToken,
  }) {
    final params = <String, dynamic>{
      if (storeId != null && storeId.isNotEmpty) 'storeId': storeId,
      if (channelId != null && channelId.isNotEmpty) 'channelId': channelId,
      if (locale != null && locale.isNotEmpty) 'locale': locale,
      if (audienceSegment != null && audienceSegment.isNotEmpty)
        'audienceSegment': audienceSegment,
      if (previewToken != null && previewToken.isNotEmpty)
        'previewToken': previewToken,
    };

    return _apiClient.get<List<HeroSlide>>(
      ApiEndpoints.heroCarousel,
      queryParameters: params.isEmpty ? null : params,
      requiresAuth: false,
      parser: (Object? value) {
        final list = _parseList(value);
        return list.map(HeroSlide.fromJson).toList(growable: false);
      },
    );
  }

  @override
  Future<List<CategoryNode>> getCategories({String? locale}) {
    final params = <String, dynamic>{
      if (locale != null && locale.isNotEmpty) 'locale': locale,
    };

    return _apiClient.get<List<CategoryNode>>(
      ApiEndpoints.categoriesTree,
      queryParameters: params.isEmpty ? null : params,
      requiresAuth: false,
      parser: (Object? value) {
        final list = _parseList(value);
        return list.map(CategoryNode.fromJson).toList(growable: false);
      },
    );
  }

  @override
  Future<PagedResult<ProductCard>> getProducts({
    String? query,
    String? categoryId,
    String sort = 'featured',
    int page = 0,
    int size = 12,
  }) {
    final params = <String, dynamic>{
      if (query != null && query.trim().isNotEmpty) 'query': query.trim(),
      if (categoryId != null && categoryId.isNotEmpty) 'categoryId': categoryId,
      'sort': sort,
      'page': page,
      'size': size,
    };

    return _apiClient.get<PagedResult<ProductCard>>(
      ApiEndpoints.merchandisingProducts,
      queryParameters: params,
      requiresAuth: false,
      parser: (Object? value) {
        final map = _parseMap(value);
        final items = _parseList(
          map['content'],
        ).map(ProductCard.fromJson).toList(growable: false);
        return PagedResult<ProductCard>(
          items: items,
          page: _toInt(map['page']),
          size: _toInt(map['size']),
          totalElements: _toInt(map['totalElements']),
          totalPages: _toInt(map['totalPages']),
          first: map['first'] == true,
          last: map['last'] == true,
        );
      },
    );
  }

  @override
  Future<ProductDetail> getProductDetail({required String productId}) {
    return _apiClient.get<ProductDetail>(
      ApiEndpoints.productById(productId),
      requiresAuth: false,
      parser: (Object? value) {
        return ProductDetail.fromJson(_parseMap(value));
      },
    );
  }

  @override
  Future<List<ProductReview>> getProductReviews({required String productId}) {
    return _apiClient.get<List<ProductReview>>(
      ApiEndpoints.productReviews(productId),
      requiresAuth: false,
      parser: (Object? value) {
        final list = _parseList(value);
        return list.map(ProductReview.fromJson).toList(growable: false);
      },
    );
  }

  @override
  Future<ProductReview> addProductReview({
    required String productId,
    required int rating,
    required String comment,
  }) {
    final requestDto = CreateProductReviewRequestDto(
      rating: rating,
      comment: comment,
    );
    return _apiClient.post<ProductReview>(
      ApiEndpoints.productReviews(productId),
      requiresAuth: true,
      body: requestDto.toJson(),
      parser: (Object? value) => ProductReview.fromJson(_parseMap(value)),
    );
  }

  @override
  Future<List<RecommendationProduct>> getTrending({int limit = 8}) {
    return _loadRecommendations(
      endpoint: ApiEndpoints.recommendationsTrending,
      limit: limit,
    );
  }

  @override
  Future<List<RecommendationProduct>> getBestSellers({int limit = 8}) {
    return _loadRecommendations(
      endpoint: ApiEndpoints.recommendationsBestSellers,
      limit: limit,
    );
  }

  @override
  Future<List<RecommendationProduct>> getDeals({int limit = 8}) {
    return _loadRecommendations(
      endpoint: ApiEndpoints.recommendationsDeals,
      limit: limit,
    );
  }

  @override
  Future<List<SearchSuggestion>> predictiveSearch({
    required String query,
    String scope = 'all',
  }) {
    return _apiClient.get<List<SearchSuggestion>>(
      ApiEndpoints.predictiveSearch,
      queryParameters: <String, dynamic>{'q': query, 'scope': scope},
      requiresAuth: false,
      parser: (Object? value) {
        final list = _parseList(value);
        return list.map(SearchSuggestion.fromJson).toList(growable: false);
      },
    );
  }

  @override
  Future<List<TrendTag>> getTrendTags() {
    return _apiClient.get<List<TrendTag>>(
      ApiEndpoints.trendTags,
      requiresAuth: false,
      parser: (Object? value) {
        final list = _parseList(value);
        return list.map(TrendTag.fromJson).toList(growable: false);
      },
    );
  }

  Future<List<RecommendationProduct>> _loadRecommendations({
    required String endpoint,
    required int limit,
  }) {
    return _apiClient.get<List<RecommendationProduct>>(
      endpoint,
      queryParameters: <String, dynamic>{'limit': limit},
      requiresAuth: false,
      parser: (Object? value) {
        final list = _parseList(value);
        return list.map(RecommendationProduct.fromJson).toList(growable: false);
      },
    );
  }

  List<Map<String, dynamic>> _parseList(Object? value) {
    if (value is List<dynamic>) {
      return value.whereType<Map<String, dynamic>>().toList(growable: false);
    }
    return const <Map<String, dynamic>>[];
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

  int _toInt(Object? value) {
    if (value is num) {
      return value.toInt();
    }
    if (value is String) {
      return int.tryParse(value) ?? 0;
    }
    return 0;
  }
}

final commerceRepositoryProvider = Provider<CommerceRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return CommerceRepositoryImpl(apiClient: apiClient);
});

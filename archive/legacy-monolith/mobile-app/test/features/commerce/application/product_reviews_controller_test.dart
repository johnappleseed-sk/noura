import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:noura/features/commerce/application/product_reviews_controller.dart';
import 'package:noura/features/commerce/application/product_reviews_state.dart';
import 'package:noura/features/commerce/data/repositories/commerce_repository_impl.dart';
import 'package:noura/features/commerce/domain/entities/category_node.dart';
import 'package:noura/features/commerce/domain/entities/hero_slide.dart';
import 'package:noura/features/commerce/domain/entities/paged_result.dart';
import 'package:noura/features/commerce/domain/entities/product_card.dart';
import 'package:noura/features/commerce/domain/entities/product_detail.dart';
import 'package:noura/features/commerce/domain/entities/product_review.dart';
import 'package:noura/features/commerce/domain/entities/recommendation_product.dart';
import 'package:noura/features/commerce/domain/entities/search_suggestion.dart';
import 'package:noura/features/commerce/domain/entities/trend_tag.dart';
import 'package:noura/features/commerce/domain/repositories/commerce_repository.dart';

class FakeCommerceRepository implements CommerceRepository {
  List<ProductReview> reviews = const <ProductReview>[
    ProductReview(
      id: 'r-1',
      userId: 'u-1',
      userName: 'Alice',
      rating: 4,
      comment: 'Nice quality',
    ),
  ];

  @override
  Future<List<ProductReview>> getProductReviews({required String productId}) {
    return Future<List<ProductReview>>.value(reviews);
  }

  @override
  Future<ProductReview> addProductReview({
    required String productId,
    required int rating,
    required String comment,
  }) async {
    final review = ProductReview(
      id: 'r-${reviews.length + 1}',
      userId: 'u-new',
      userName: 'Reviewer',
      rating: rating,
      comment: comment.trim(),
    );
    reviews = <ProductReview>[review, ...reviews];
    return review;
  }

  @override
  Future<List<HeroSlide>> getHeroSlides({
    String? storeId,
    String? channelId,
    String? locale,
    String? audienceSegment,
    String? previewToken,
  }) {
    return Future<List<HeroSlide>>.value(const <HeroSlide>[]);
  }

  @override
  Future<List<CategoryNode>> getCategories({String? locale}) {
    return Future<List<CategoryNode>>.value(const <CategoryNode>[]);
  }

  @override
  Future<PagedResult<ProductCard>> getProducts({
    String? query,
    String? categoryId,
    String sort = 'featured',
    int page = 0,
    int size = 12,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<ProductDetail> getProductDetail({required String productId}) {
    throw UnimplementedError();
  }

  @override
  Future<List<RecommendationProduct>> getTrending({int limit = 8}) {
    return Future<List<RecommendationProduct>>.value(
      const <RecommendationProduct>[],
    );
  }

  @override
  Future<List<RecommendationProduct>> getBestSellers({int limit = 8}) {
    return Future<List<RecommendationProduct>>.value(
      const <RecommendationProduct>[],
    );
  }

  @override
  Future<List<RecommendationProduct>> getDeals({int limit = 8}) {
    return Future<List<RecommendationProduct>>.value(
      const <RecommendationProduct>[],
    );
  }

  @override
  Future<List<SearchSuggestion>> predictiveSearch({
    required String query,
    String scope = 'all',
  }) {
    return Future<List<SearchSuggestion>>.value(const <SearchSuggestion>[]);
  }

  @override
  Future<List<TrendTag>> getTrendTags() {
    return Future<List<TrendTag>>.value(const <TrendTag>[]);
  }
}

void main() {
  group('ProductReviewsController', () {
    test('loads reviews and submits a new review', () async {
      final container = ProviderContainer(
        overrides: [
          commerceRepositoryProvider.overrideWithValue(
            FakeCommerceRepository(),
          ),
        ],
      );
      addTearDown(container.dispose);

      final initial = await container.read(
        productReviewsControllerProvider('p-1').future,
      );
      expect(initial.items, hasLength(1));
      expect(initial.averageRating, closeTo(4.0, 0.001));

      final success = await container
          .read(productReviewsControllerProvider('p-1').notifier)
          .submitReview(rating: 5, comment: 'Excellent');
      expect(success, isTrue);

      final AsyncValue<ProductReviewsState> current = container.read(
        productReviewsControllerProvider('p-1'),
      );
      expect(current.value?.items, hasLength(2));
      expect(current.value?.items.first.rating, 5);
      expect(current.value?.actionMessage, 'Review submitted.');
    });

    test('validates blank review comment', () async {
      final container = ProviderContainer(
        overrides: [
          commerceRepositoryProvider.overrideWithValue(
            FakeCommerceRepository(),
          ),
        ],
      );
      addTearDown(container.dispose);

      await container.read(productReviewsControllerProvider('p-1').future);
      final success = await container
          .read(productReviewsControllerProvider('p-1').notifier)
          .submitReview(rating: 5, comment: '   ');

      expect(success, isFalse);
      final current = container.read(productReviewsControllerProvider('p-1'));
      expect(current.value?.actionError, 'Review comment is required.');
    });
  });
}

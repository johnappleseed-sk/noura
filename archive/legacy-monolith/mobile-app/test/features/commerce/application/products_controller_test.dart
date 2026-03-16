import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:noura/features/commerce/application/product_list_filter.dart';
import 'package:noura/features/commerce/application/products_controller.dart';
import 'package:noura/features/commerce/application/products_state.dart';
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
  @override
  Future<List<HeroSlide>> getHeroSlides({
    String? storeId,
    String? channelId,
    String? locale,
    String? audienceSegment,
    String? previewToken,
  }) async {
    return const <HeroSlide>[];
  }

  @override
  Future<List<CategoryNode>> getCategories({String? locale}) async {
    return const <CategoryNode>[];
  }

  @override
  Future<PagedResult<ProductCard>> getProducts({
    String? query,
    String? categoryId,
    String sort = 'featured',
    int page = 0,
    int size = 12,
  }) async {
    if (page == 0) {
      return PagedResult<ProductCard>(
        items: const <ProductCard>[
          ProductCard(
            id: 'p-1',
            name: 'Product 1',
            categoryId: 'c-1',
            categoryName: 'Category',
            price: 10,
            compareAtPrice: null,
            imageUrl: '',
            stockQty: 5,
            lowStock: false,
            allowNegativeStock: false,
            isNew: false,
            isTrending: true,
            isBestseller: false,
            merchandisingScore: 1,
          ),
        ],
        page: 0,
        size: 12,
        totalElements: 2,
        totalPages: 2,
        first: true,
        last: false,
      );
    }

    return PagedResult<ProductCard>(
      items: const <ProductCard>[
        ProductCard(
          id: 'p-2',
          name: 'Product 2',
          categoryId: 'c-1',
          categoryName: 'Category',
          price: 20,
          compareAtPrice: null,
          imageUrl: '',
          stockQty: 6,
          lowStock: false,
          allowNegativeStock: false,
          isNew: false,
          isTrending: false,
          isBestseller: true,
          merchandisingScore: 2,
        ),
      ],
      page: 1,
      size: 12,
      totalElements: 2,
      totalPages: 2,
      first: false,
      last: true,
    );
  }

  @override
  Future<ProductDetail> getProductDetail({required String productId}) async {
    throw UnimplementedError();
  }

  @override
  Future<List<ProductReview>> getProductReviews({required String productId}) {
    return Future<List<ProductReview>>.value(const <ProductReview>[]);
  }

  @override
  Future<ProductReview> addProductReview({
    required String productId,
    required int rating,
    required String comment,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<List<RecommendationProduct>> getTrending({int limit = 8}) async {
    return const <RecommendationProduct>[];
  }

  @override
  Future<List<RecommendationProduct>> getBestSellers({int limit = 8}) async {
    return const <RecommendationProduct>[];
  }

  @override
  Future<List<RecommendationProduct>> getDeals({int limit = 8}) async {
    return const <RecommendationProduct>[];
  }

  @override
  Future<List<SearchSuggestion>> predictiveSearch({
    required String query,
    String scope = 'all',
  }) async {
    return const <SearchSuggestion>[];
  }

  @override
  Future<List<TrendTag>> getTrendTags() async {
    return const <TrendTag>[];
  }
}

void main() {
  test('ProductsController loads first page and paginates', () async {
    final container = ProviderContainer(
      overrides: [
        commerceRepositoryProvider.overrideWithValue(FakeCommerceRepository()),
      ],
    );
    addTearDown(container.dispose);

    const filter = ProductListFilter();
    final initial = await container.read(
      productsControllerProvider(filter).future,
    );

    expect(initial.items, hasLength(1));
    expect(initial.hasNext, isTrue);

    await container
        .read(productsControllerProvider(filter).notifier)
        .loadMore();

    final AsyncValue<ProductsState> current = container.read(
      productsControllerProvider(filter),
    );
    expect(current.value?.items, hasLength(2));
    expect(current.value?.hasNext, isFalse);
  });
}

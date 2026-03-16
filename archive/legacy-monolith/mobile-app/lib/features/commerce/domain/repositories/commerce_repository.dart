import '../entities/category_node.dart';
import '../entities/hero_slide.dart';
import '../entities/paged_result.dart';
import '../entities/product_card.dart';
import '../entities/product_detail.dart';
import '../entities/product_review.dart';
import '../entities/recommendation_product.dart';
import '../entities/search_suggestion.dart';
import '../entities/trend_tag.dart';

abstract class CommerceRepository {
  Future<List<HeroSlide>> getHeroSlides({
    String? storeId,
    String? channelId,
    String? locale,
    String? audienceSegment,
    String? previewToken,
  });

  Future<List<CategoryNode>> getCategories({String? locale});

  Future<PagedResult<ProductCard>> getProducts({
    String? query,
    String? categoryId,
    String sort = 'featured',
    int page = 0,
    int size = 12,
  });

  Future<ProductDetail> getProductDetail({required String productId});

  Future<List<ProductReview>> getProductReviews({required String productId});

  Future<ProductReview> addProductReview({
    required String productId,
    required int rating,
    required String comment,
  });

  Future<List<RecommendationProduct>> getTrending({int limit = 8});

  Future<List<RecommendationProduct>> getBestSellers({int limit = 8});

  Future<List<RecommendationProduct>> getDeals({int limit = 8});

  Future<List<SearchSuggestion>> predictiveSearch({
    required String query,
    String scope = 'all',
  });

  Future<List<TrendTag>> getTrendTags();
}

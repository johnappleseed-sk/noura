import '../domain/entities/category_node.dart';
import '../domain/entities/hero_slide.dart';
import '../domain/entities/recommendation_product.dart';

class HomeFeedState {
  const HomeFeedState({
    required this.heroSlides,
    required this.categories,
    required this.trending,
    required this.bestSellers,
    required this.deals,
  });

  final List<HeroSlide> heroSlides;
  final List<CategoryNode> categories;
  final List<RecommendationProduct> trending;
  final List<RecommendationProduct> bestSellers;
  final List<RecommendationProduct> deals;
}

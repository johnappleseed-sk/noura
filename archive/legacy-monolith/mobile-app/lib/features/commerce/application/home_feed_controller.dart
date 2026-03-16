import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/constants/app_constants.dart';
import '../data/repositories/commerce_repository_impl.dart';
import '../domain/entities/category_node.dart';
import '../domain/entities/hero_slide.dart';
import '../domain/entities/recommendation_product.dart';
import 'home_feed_state.dart';

class HomeFeedController extends AsyncNotifier<HomeFeedState> {
  @override
  Future<HomeFeedState> build() async {
    return _load();
  }

  Future<void> refresh() async {
    state = const AsyncLoading<HomeFeedState>();
    state = await AsyncValue.guard<HomeFeedState>(_load);
  }

  Future<HomeFeedState> _load() async {
    final repository = ref.read(commerceRepositoryProvider);

    final futures = await Future.wait<Object>([
      repository.getHeroSlides(),
      repository.getCategories(),
      repository.getTrending(limit: AppConstants.defaultRecommendationSize),
      repository.getBestSellers(limit: AppConstants.defaultRecommendationSize),
      repository.getDeals(limit: AppConstants.defaultRecommendationSize),
    ]);

    final rootCategories = futures[1] as List<CategoryNode>;
    final flatCategories = rootCategories
        .expand((CategoryNode node) => node.flatten())
        .toList(growable: false);

    return HomeFeedState(
      heroSlides: futures[0] as List<HeroSlide>,
      categories: flatCategories,
      trending: futures[2] as List<RecommendationProduct>,
      bestSellers: futures[3] as List<RecommendationProduct>,
      deals: futures[4] as List<RecommendationProduct>,
    );
  }
}

final homeFeedControllerProvider =
    AsyncNotifierProvider<HomeFeedController, HomeFeedState>(
      HomeFeedController.new,
    );

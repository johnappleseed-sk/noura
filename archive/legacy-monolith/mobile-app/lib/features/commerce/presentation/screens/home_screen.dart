import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/app_empty_view.dart';
import '../../../../core/widgets/app_error_view.dart';
import '../../../../core/widgets/app_loading_view.dart';
import '../../application/home_feed_controller.dart';
import '../../domain/entities/hero_slide.dart';
import '../../domain/entities/recommendation_product.dart';
import '../widgets/commerce_product_tile.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final feedState = ref.watch(homeFeedControllerProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Noura'),
        actions: [
          IconButton(
            onPressed: () => context.go(AppRoutes.searchPath),
            icon: const Icon(Icons.search),
          ),
          IconButton(
            onPressed: () => context.go(AppRoutes.profilePath),
            icon: const Icon(Icons.person_outline),
          ),
          IconButton(
            onPressed: () => context.go(AppRoutes.wishlistPath),
            icon: const Icon(Icons.favorite_border_outlined),
          ),
          IconButton(
            onPressed: () => context.go(AppRoutes.cartPath),
            icon: const Icon(Icons.shopping_cart_outlined),
          ),
        ],
      ),
      body: feedState.when(
        loading: () => const AppLoadingView(message: 'Loading home feed...'),
        error: (Object error, StackTrace stackTrace) => AppErrorView(
          message: error.toString(),
          onRetry: () =>
              ref.read(homeFeedControllerProvider.notifier).refresh(),
        ),
        data: (state) => RefreshIndicator(
          onRefresh: () =>
              ref.read(homeFeedControllerProvider.notifier).refresh(),
          child: ListView(
            padding: const EdgeInsets.only(bottom: 20),
            children: [
              if (state.heroSlides.isNotEmpty)
                _HeroCarousel(slides: state.heroSlides),
              const SizedBox(height: 12),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Categories',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                    TextButton(
                      onPressed: () => context.go(AppRoutes.categoriesPath),
                      child: const Text('See all'),
                    ),
                  ],
                ),
              ),
              if (state.categories.isEmpty)
                const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 16),
                  child: AppEmptyView(
                    title: 'No categories available.',
                    subtitle: 'Try again later.',
                  ),
                )
              else
                SizedBox(
                  height: 44,
                  child: ListView.separated(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    scrollDirection: Axis.horizontal,
                    itemBuilder: (context, index) {
                      final category = state.categories[index];
                      return ActionChip(
                        label: Text(category.name),
                        onPressed: () => context.go(
                          '${AppRoutes.productsPath}'
                          '?categoryId=${Uri.encodeComponent(category.id)}'
                          '&categoryName=${Uri.encodeComponent(category.name)}',
                        ),
                      );
                    },
                    separatorBuilder: (BuildContext context, int index) =>
                        const SizedBox(width: 8),
                    itemCount: state.categories.length.clamp(0, 10).toInt(),
                  ),
                ),
              const SizedBox(height: 16),
              _RecommendationSection(
                title: 'Trending',
                products: state.trending,
              ),
              _RecommendationSection(
                title: 'Best Sellers',
                products: state.bestSellers,
              ),
              _RecommendationSection(title: 'Deals', products: state.deals),
            ],
          ),
        ),
      ),
    );
  }
}

class _HeroCarousel extends StatefulWidget {
  const _HeroCarousel({required this.slides});

  final List<HeroSlide> slides;

  @override
  State<_HeroCarousel> createState() => _HeroCarouselState();
}

class _HeroCarouselState extends State<_HeroCarousel> {
  final PageController _pageController = PageController(viewportFraction: 0.92);
  int _currentPage = 0;

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        SizedBox(
          height: 190,
          child: PageView.builder(
            controller: _pageController,
            onPageChanged: (value) => setState(() => _currentPage = value),
            itemCount: widget.slides.length,
            itemBuilder: (context, index) {
              final slide = widget.slides[index];
              return Padding(
                padding: const EdgeInsets.symmetric(horizontal: 6),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(18),
                  child: Stack(
                    fit: StackFit.expand,
                    children: [
                      Image.network(
                        slide.bestImageUrl,
                        fit: BoxFit.cover,
                        errorBuilder:
                            (
                              BuildContext context,
                              Object error,
                              StackTrace? stackTrace,
                            ) => Container(
                              color: Theme.of(
                                context,
                              ).colorScheme.surfaceContainerHighest,
                            ),
                      ),
                      DecoratedBox(
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            begin: Alignment.bottomCenter,
                            end: Alignment.topCenter,
                            colors: [
                              Colors.black.withValues(alpha: 0.65),
                              Colors.transparent,
                            ],
                          ),
                        ),
                      ),
                      Padding(
                        padding: const EdgeInsets.all(14),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisAlignment: MainAxisAlignment.end,
                          children: [
                            Text(
                              slide.title,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: Theme.of(context).textTheme.titleLarge
                                  ?.copyWith(
                                    color: Colors.white,
                                    fontWeight: FontWeight.w700,
                                  ),
                            ),
                            if (slide.buttonText.isNotEmpty) ...[
                              const SizedBox(height: 8),
                              FilledButton(
                                onPressed: () {
                                  context.go(AppRoutes.productsPath);
                                },
                                child: Text(slide.buttonText),
                              ),
                            ],
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
        ),
        const SizedBox(height: 10),
        Wrap(
          spacing: 6,
          children: List.generate(
            widget.slides.length,
            (index) => AnimatedContainer(
              duration: const Duration(milliseconds: 220),
              width: _currentPage == index ? 18 : 8,
              height: 8,
              decoration: BoxDecoration(
                color: _currentPage == index
                    ? Theme.of(context).colorScheme.primary
                    : Theme.of(context).colorScheme.outlineVariant,
                borderRadius: BorderRadius.circular(6),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _RecommendationSection extends StatelessWidget {
  const _RecommendationSection({required this.title, required this.products});

  final String title;
  final List<RecommendationProduct> products;

  @override
  Widget build(BuildContext context) {
    final formatter = NumberFormat.currency(symbol: '\$');

    return Padding(
      padding: const EdgeInsets.only(bottom: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Text(title, style: Theme.of(context).textTheme.titleLarge),
          ),
          const SizedBox(height: 10),
          if (products.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 16),
              child: AppEmptyView(title: 'No products available yet.'),
            )
          else
            SizedBox(
              height: 245,
              child: ListView.separated(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                scrollDirection: Axis.horizontal,
                itemCount: products.length,
                separatorBuilder: (BuildContext context, int index) =>
                    const SizedBox(width: 10),
                itemBuilder: (context, index) {
                  final product = products[index];
                  return SizedBox(
                    width: 190,
                    child: CommerceProductTile(
                      name: product.name,
                      subtitle: product.categoryName,
                      imageUrl: product.imageUrl,
                      priceLabel: formatter.format(product.price),
                      badge: product.reason.isNotEmpty ? product.reason : null,
                      onTap: () => context.go(
                        AppRoutes.productDetailPath.replaceFirst(
                          ':productId',
                          product.id,
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
        ],
      ),
    );
  }
}

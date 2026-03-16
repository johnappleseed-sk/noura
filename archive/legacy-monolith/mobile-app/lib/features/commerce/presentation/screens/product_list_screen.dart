import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/app_empty_view.dart';
import '../../../../core/widgets/app_error_view.dart';
import '../../../../core/widgets/app_loading_view.dart';
import '../../application/product_list_filter.dart';
import '../../application/products_controller.dart';
import '../widgets/commerce_product_tile.dart';

class ProductListScreen extends ConsumerStatefulWidget {
  const ProductListScreen({
    super.key,
    this.initialQuery,
    this.initialCategoryId,
    this.initialCategoryName,
  });

  final String? initialQuery;
  final String? initialCategoryId;
  final String? initialCategoryName;

  @override
  ConsumerState<ProductListScreen> createState() => _ProductListScreenState();
}

class _ProductListScreenState extends ConsumerState<ProductListScreen> {
  static const _sortOptions = <String, String>{
    'featured': 'Featured',
    'priceAsc': 'Price: Low to High',
    'priceDesc': 'Price: High to Low',
    'trending': 'Trending',
    'new': 'New Arrivals',
  };

  late final TextEditingController _searchController;
  late ProductListFilter _filter;
  late String _selectedSort;

  @override
  void initState() {
    super.initState();
    _searchController = TextEditingController(text: widget.initialQuery ?? '');
    _selectedSort = 'featured';
    _filter = ProductListFilter(
      query: widget.initialQuery?.trim().isEmpty == true
          ? null
          : widget.initialQuery?.trim(),
      categoryId: widget.initialCategoryId,
      sort: _selectedSort,
    );
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _applySearch() {
    setState(() {
      _filter = _filter.copyWith(
        query: _searchController.text.trim().isEmpty
            ? null
            : _searchController.text.trim(),
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    final productsState = ref.watch(productsControllerProvider(_filter));
    final formatter = NumberFormat.currency(symbol: '\$');

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.initialCategoryName ?? 'Products'),
        actions: [
          IconButton(
            onPressed: () => context.go(AppRoutes.searchPath),
            icon: const Icon(Icons.search),
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: TextField(
              controller: _searchController,
              textInputAction: TextInputAction.search,
              onSubmitted: (_) => _applySearch(),
              decoration: InputDecoration(
                hintText: 'Search products...',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: IconButton(
                  onPressed: _applySearch,
                  icon: const Icon(Icons.arrow_forward),
                ),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 10),
            child: Row(
              children: [
                Expanded(
                  child: DropdownButtonFormField<String>(
                    initialValue: _selectedSort,
                    items: _sortOptions.entries
                        .map(
                          (entry) => DropdownMenuItem<String>(
                            value: entry.key,
                            child: Text(entry.value),
                          ),
                        )
                        .toList(growable: false),
                    onChanged: (value) {
                      if (value == null) {
                        return;
                      }
                      setState(() {
                        _selectedSort = value;
                        _filter = _filter.copyWith(sort: value);
                      });
                    },
                    decoration: const InputDecoration(labelText: 'Sort by'),
                  ),
                ),
                if (_filter.categoryId != null) ...[
                  const SizedBox(width: 10),
                  ActionChip(
                    label: const Text('Clear category'),
                    onPressed: () {
                      setState(() {
                        _filter = ProductListFilter(
                          query: _filter.query,
                          categoryId: null,
                          sort: _filter.sort,
                          pageSize: _filter.pageSize,
                        );
                      });
                    },
                  ),
                ],
              ],
            ),
          ),
          Expanded(
            child: productsState.when(
              loading: () =>
                  const AppLoadingView(message: 'Loading products...'),
              error: (Object error, StackTrace stackTrace) => AppErrorView(
                message: error.toString(),
                onRetry: () {
                  ref.invalidate(productsControllerProvider(_filter));
                },
              ),
              data: (state) {
                if (state.items.isEmpty) {
                  return const AppEmptyView(
                    title: 'No products found.',
                    subtitle: 'Try another search keyword or category.',
                  );
                }

                return RefreshIndicator(
                  onRefresh: () async {
                    ref.invalidate(productsControllerProvider(_filter));
                    await ref.read(productsControllerProvider(_filter).future);
                  },
                  child: CustomScrollView(
                    slivers: [
                      SliverPadding(
                        padding: const EdgeInsets.symmetric(horizontal: 12),
                        sliver: SliverGrid(
                          gridDelegate:
                              const SliverGridDelegateWithFixedCrossAxisCount(
                                crossAxisCount: 2,
                                mainAxisSpacing: 10,
                                crossAxisSpacing: 10,
                                childAspectRatio: 0.60,
                              ),
                          delegate: SliverChildBuilderDelegate((
                            context,
                            index,
                          ) {
                            final product = state.items[index];
                            return CommerceProductTile(
                              name: product.name,
                              subtitle: product.categoryName,
                              imageUrl: product.imageUrl,
                              priceLabel: formatter.format(product.price),
                              badge: product.isNew
                                  ? 'New'
                                  : product.isTrending
                                  ? 'Trending'
                                  : null,
                              onTap: () => context.go(
                                AppRoutes.productDetailPath.replaceFirst(
                                  ':productId',
                                  product.id,
                                ),
                              ),
                            );
                          }, childCount: state.items.length),
                        ),
                      ),
                      SliverToBoxAdapter(
                        child: Padding(
                          padding: const EdgeInsets.fromLTRB(16, 14, 16, 24),
                          child: Column(
                            children: [
                              if (state.paginationError != null)
                                Padding(
                                  padding: const EdgeInsets.only(bottom: 8),
                                  child: Text(
                                    state.paginationError!,
                                    style: TextStyle(
                                      color: Theme.of(
                                        context,
                                      ).colorScheme.error,
                                    ),
                                    textAlign: TextAlign.center,
                                  ),
                                ),
                              if (state.hasNext)
                                FilledButton.tonal(
                                  onPressed: state.isLoadingMore
                                      ? null
                                      : () => ref
                                            .read(
                                              productsControllerProvider(
                                                _filter,
                                              ).notifier,
                                            )
                                            .loadMore(),
                                  child: state.isLoadingMore
                                      ? const SizedBox(
                                          width: 18,
                                          height: 18,
                                          child: CircularProgressIndicator(
                                            strokeWidth: 2,
                                          ),
                                        )
                                      : const Text('Load more'),
                                )
                              else
                                Text(
                                  'You have reached the end.',
                                  style: Theme.of(context).textTheme.bodySmall,
                                ),
                            ],
                          ),
                        ),
                      ),
                    ],
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

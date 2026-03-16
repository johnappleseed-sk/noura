import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/error/app_exception.dart';
import '../data/repositories/commerce_repository_impl.dart';
import '../domain/entities/product_card.dart';
import 'product_list_filter.dart';
import 'products_state.dart';

class ProductsController
    extends FamilyAsyncNotifier<ProductsState, ProductListFilter> {
  late ProductListFilter _filter;

  @override
  Future<ProductsState> build(ProductListFilter arg) async {
    _filter = arg;
    return _loadPage(page: 0, append: false);
  }

  Future<void> applyFilter(ProductListFilter filter) async {
    _filter = filter;
    state = const AsyncLoading<ProductsState>();
    state = await AsyncValue.guard<ProductsState>(
      () => _loadPage(page: 0, append: false),
    );
  }

  Future<void> loadMore() async {
    final current = state.valueOrNull;
    if (current == null || current.isLoadingMore || !current.hasNext) {
      return;
    }

    state = AsyncData<ProductsState>(
      current.copyWith(isLoadingMore: true, paginationError: null),
    );

    try {
      final nextState = await _loadPage(page: current.page + 1, append: true);
      state = AsyncData<ProductsState>(nextState);
    } on AppException catch (error) {
      state = AsyncData<ProductsState>(
        current.copyWith(isLoadingMore: false, paginationError: error.message),
      );
    }
  }

  Future<ProductsState> _loadPage({
    required int page,
    required bool append,
  }) async {
    final repository = ref.read(commerceRepositoryProvider);
    final paged = await repository.getProducts(
      query: _filter.query,
      categoryId: _filter.categoryId,
      sort: _filter.sort,
      page: page,
      size: _filter.pageSize,
    );

    final existing = append
        ? (state.valueOrNull?.items ?? const <ProductCard>[])
        : const <ProductCard>[];
    final items = <ProductCard>[...existing, ...paged.items];

    return ProductsState(
      filter: _filter,
      items: items,
      page: paged.page,
      hasNext: paged.hasNext,
      isLoadingMore: false,
      paginationError: null,
    );
  }
}

final productsControllerProvider =
    AsyncNotifierProvider.family<
      ProductsController,
      ProductsState,
      ProductListFilter
    >(ProductsController.new);

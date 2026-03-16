import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/error/error_presenter.dart';
import '../../commerce/data/repositories/commerce_repository_impl.dart';
import '../../commerce/domain/entities/product_detail.dart';
import '../data/repositories/wishlist_repository_impl.dart';
import 'wishlist_state.dart';

class WishlistController extends AsyncNotifier<WishlistState> {
  @override
  Future<WishlistState> build() async {
    return _load();
  }

  Future<void> refresh() async {
    state = const AsyncLoading<WishlistState>();
    state = await AsyncValue.guard<WishlistState>(_load);
  }

  Future<bool> toggle(String productId) async {
    final current = state.valueOrNull ?? const WishlistState.initial();
    state = AsyncData<WishlistState>(
      current.copyWith(
        isMutating: true,
        actionMessage: null,
        actionError: null,
      ),
    );

    try {
      final nextIds = await ref
          .read(wishlistRepositoryProvider)
          .toggle(productId);
      final nextProducts = await _resolveProducts(nextIds);
      final wasSaved = current.productIds.contains(productId);
      state = AsyncData<WishlistState>(
        WishlistState(
          productIds: nextIds,
          products: nextProducts,
          isMutating: false,
          actionMessage: wasSaved
              ? 'Removed from wishlist.'
              : 'Added to wishlist.',
        ),
      );
      return true;
    } on Object catch (error) {
      state = AsyncData<WishlistState>(
        current.copyWith(
          isMutating: false,
          actionError: ErrorPresenter.message(error),
          actionMessage: null,
        ),
      );
      return false;
    }
  }

  Future<bool> remove(String productId) async {
    final current = state.valueOrNull ?? const WishlistState.initial();
    state = AsyncData<WishlistState>(
      current.copyWith(
        isMutating: true,
        actionMessage: null,
        actionError: null,
      ),
    );

    try {
      final nextIds = await ref
          .read(wishlistRepositoryProvider)
          .remove(productId);
      final nextProducts = await _resolveProducts(nextIds);
      state = AsyncData<WishlistState>(
        WishlistState(
          productIds: nextIds,
          products: nextProducts,
          isMutating: false,
          actionMessage: 'Removed from wishlist.',
        ),
      );
      return true;
    } on Object catch (error) {
      state = AsyncData<WishlistState>(
        current.copyWith(
          isMutating: false,
          actionError: ErrorPresenter.message(error),
          actionMessage: null,
        ),
      );
      return false;
    }
  }

  bool contains(String productId) {
    return state.valueOrNull?.productIds.contains(productId) ?? false;
  }

  void clearMessages() {
    final current = state.valueOrNull;
    if (current == null) {
      return;
    }
    state = AsyncData<WishlistState>(
      current.copyWith(actionError: null, actionMessage: null),
    );
  }

  Future<WishlistState> _load() async {
    final ids = await ref.read(wishlistRepositoryProvider).getProductIds();
    final products = await _resolveProducts(ids);
    return WishlistState(
      productIds: ids,
      products: products,
      isMutating: false,
    );
  }

  Future<List<ProductDetail>> _resolveProducts(Set<String> ids) async {
    if (ids.isEmpty) {
      return const <ProductDetail>[];
    }

    final repository = ref.read(commerceRepositoryProvider);
    final resolvedProducts = <ProductDetail>[];
    final staleIds = <String>{};

    for (final id in ids) {
      try {
        final product = await repository.getProductDetail(productId: id);
        if (product.id.isEmpty) {
          staleIds.add(id);
          continue;
        }
        resolvedProducts.add(product);
      } on Object {
        staleIds.add(id);
      }
    }

    if (staleIds.isNotEmpty) {
      final cleanedIds = <String>{...ids}..removeAll(staleIds);
      await ref.read(wishlistRepositoryProvider).saveProductIds(cleanedIds);
    }

    return resolvedProducts;
  }
}

final wishlistControllerProvider =
    AsyncNotifierProvider<WishlistController, WishlistState>(
      WishlistController.new,
    );

final wishlistContainsProvider = Provider.family<bool, String>((
  ref,
  productId,
) {
  final state = ref.watch(wishlistControllerProvider);
  return state.valueOrNull?.productIds.contains(productId) ?? false;
});

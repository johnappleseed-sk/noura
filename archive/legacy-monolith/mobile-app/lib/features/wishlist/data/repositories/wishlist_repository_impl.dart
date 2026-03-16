import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/repositories/wishlist_repository.dart';
import '../local/wishlist_local_store.dart';

class WishlistRepositoryImpl implements WishlistRepository {
  WishlistRepositoryImpl({required WishlistLocalStore localStore})
    : _localStore = localStore;

  final WishlistLocalStore _localStore;

  @override
  Future<Set<String>> getProductIds() {
    return _localStore.readIds();
  }

  @override
  Future<Set<String>> saveProductIds(Set<String> ids) {
    return _localStore.writeIds(ids);
  }

  @override
  Future<Set<String>> add(String productId) async {
    final ids = await _localStore.readIds();
    ids.add(productId);
    return _localStore.writeIds(ids);
  }

  @override
  Future<Set<String>> remove(String productId) async {
    final ids = await _localStore.readIds();
    ids.remove(productId);
    return _localStore.writeIds(ids);
  }

  @override
  Future<Set<String>> toggle(String productId) async {
    final ids = await _localStore.readIds();
    if (ids.contains(productId)) {
      ids.remove(productId);
    } else {
      ids.add(productId);
    }
    return _localStore.writeIds(ids);
  }
}

final wishlistLocalStoreProvider = Provider<WishlistLocalStore>((ref) {
  return WishlistLocalStore();
});

final wishlistRepositoryProvider = Provider<WishlistRepository>((ref) {
  final localStore = ref.watch(wishlistLocalStoreProvider);
  return WishlistRepositoryImpl(localStore: localStore);
});

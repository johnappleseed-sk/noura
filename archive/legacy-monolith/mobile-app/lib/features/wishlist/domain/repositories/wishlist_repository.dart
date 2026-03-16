abstract class WishlistRepository {
  Future<Set<String>> getProductIds();

  Future<Set<String>> saveProductIds(Set<String> ids);

  Future<Set<String>> add(String productId);

  Future<Set<String>> remove(String productId);

  Future<Set<String>> toggle(String productId);
}

import '../../commerce/domain/entities/product_detail.dart';

class WishlistState {
  const WishlistState({
    required this.productIds,
    required this.products,
    required this.isMutating,
    this.actionMessage,
    this.actionError,
  });

  const WishlistState.initial()
    : productIds = const <String>{},
      products = const <ProductDetail>[],
      isMutating = false,
      actionMessage = null,
      actionError = null;

  final Set<String> productIds;
  final List<ProductDetail> products;
  final bool isMutating;
  final String? actionMessage;
  final String? actionError;

  WishlistState copyWith({
    Set<String>? productIds,
    List<ProductDetail>? products,
    bool? isMutating,
    Object? actionMessage = _unset,
    Object? actionError = _unset,
  }) {
    return WishlistState(
      productIds: productIds ?? this.productIds,
      products: products ?? this.products,
      isMutating: isMutating ?? this.isMutating,
      actionMessage: identical(actionMessage, _unset)
          ? this.actionMessage
          : actionMessage as String?,
      actionError: identical(actionError, _unset)
          ? this.actionError
          : actionError as String?,
    );
  }

  static const Object _unset = Object();
}

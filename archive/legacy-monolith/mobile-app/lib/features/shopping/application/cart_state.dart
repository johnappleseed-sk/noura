import '../domain/entities/cart.dart';

class CartState {
  const CartState({
    required this.cart,
    required this.isMutating,
    this.actionMessage,
    this.actionError,
  });

  const CartState.initial()
    : cart = const Cart.empty(),
      isMutating = false,
      actionMessage = null,
      actionError = null;

  final Cart cart;
  final bool isMutating;
  final String? actionMessage;
  final String? actionError;

  CartState copyWith({
    Cart? cart,
    bool? isMutating,
    Object? actionMessage = _unset,
    Object? actionError = _unset,
  }) {
    return CartState(
      cart: cart ?? this.cart,
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

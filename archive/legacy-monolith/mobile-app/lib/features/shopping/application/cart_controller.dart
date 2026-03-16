import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/error/app_exception.dart';
import '../../../core/error/error_presenter.dart';
import '../data/repositories/shopping_repository_impl.dart';
import 'cart_state.dart';

class CartController extends AsyncNotifier<CartState> {
  @override
  Future<CartState> build() async {
    return _load();
  }

  Future<void> refresh() async {
    state = const AsyncLoading<CartState>();
    state = await AsyncValue.guard<CartState>(_load);
  }

  Future<bool> addItem({
    required String productId,
    String? variantId,
    int quantity = 1,
    String? storeId,
  }) {
    if (productId.trim().isEmpty) {
      _setActionError('Product ID is required.');
      return Future<bool>.value(false);
    }
    if (quantity < 1) {
      _setActionError('Quantity must be at least 1.');
      return Future<bool>.value(false);
    }
    return _mutate(
      () => ref
          .read(shoppingRepositoryProvider)
          .addCartItem(
            productId: productId,
            variantId: variantId,
            quantity: quantity,
            storeId: storeId,
          ),
      successMessage: 'Product added to cart.',
    );
  }

  Future<bool> updateQuantity({
    required String cartItemId,
    required int quantity,
  }) {
    if (cartItemId.trim().isEmpty) {
      _setActionError('Cart item ID is required.');
      return Future<bool>.value(false);
    }
    if (quantity < 1) {
      _setActionError('Quantity must be at least 1.');
      return Future<bool>.value(false);
    }
    return _mutate(
      () => ref
          .read(shoppingRepositoryProvider)
          .updateCartItem(cartItemId: cartItemId, quantity: quantity),
    );
  }

  Future<bool> removeItem({required String cartItemId}) {
    return _mutate(
      () => ref
          .read(shoppingRepositoryProvider)
          .removeCartItem(cartItemId: cartItemId),
      successMessage: 'Item removed from cart.',
    );
  }

  Future<bool> clearCart() {
    return _mutate(
      () => ref.read(shoppingRepositoryProvider).clearCartItems(),
      successMessage: 'Cart cleared.',
    );
  }

  Future<bool> applyCoupon(String couponCode) {
    final trimmed = couponCode.trim();
    if (trimmed.isEmpty) {
      _setActionError('Please enter a coupon code.');
      return Future<bool>.value(false);
    }
    return _mutate(
      () =>
          ref.read(shoppingRepositoryProvider).applyCoupon(couponCode: trimmed),
      successMessage: 'Coupon applied.',
    );
  }

  void clearMessages() {
    final current = state.valueOrNull;
    if (current == null) {
      return;
    }
    state = AsyncData<CartState>(
      current.copyWith(actionError: null, actionMessage: null),
    );
  }

  void _setActionError(String message) {
    final current = state.valueOrNull ?? const CartState.initial();
    state = AsyncData<CartState>(
      current.copyWith(
        isMutating: false,
        actionError: message,
        actionMessage: null,
      ),
    );
  }

  Future<CartState> _load() async {
    final cart = await ref.read(shoppingRepositoryProvider).getCart();
    return CartState(cart: cart, isMutating: false);
  }

  Future<bool> _mutate(
    Future<dynamic> Function() operation, {
    String? successMessage,
  }) async {
    final current = state.valueOrNull;
    if (current != null) {
      state = AsyncData<CartState>(
        current.copyWith(
          isMutating: true,
          actionError: null,
          actionMessage: null,
        ),
      );
    }

    try {
      final cart = await operation();
      state = AsyncData<CartState>(
        CartState(cart: cart, isMutating: false, actionMessage: successMessage),
      );
      return true;
    } on AppException catch (error) {
      final previous = current ?? const CartState.initial();
      state = AsyncData<CartState>(
        previous.copyWith(
          isMutating: false,
          actionError: error.message,
          actionMessage: null,
        ),
      );
      return false;
    } on Object catch (error) {
      final previous = current ?? const CartState.initial();
      state = AsyncData<CartState>(
        previous.copyWith(
          isMutating: false,
          actionError: ErrorPresenter.message(error),
          actionMessage: null,
        ),
      );
      return false;
    }
  }
}

final cartControllerProvider = AsyncNotifierProvider<CartController, CartState>(
  CartController.new,
);

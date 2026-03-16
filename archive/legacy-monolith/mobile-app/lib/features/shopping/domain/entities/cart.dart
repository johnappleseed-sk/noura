import 'cart_item.dart';
import 'cart_totals.dart';

class Cart {
  const Cart({
    required this.cartId,
    required this.storeId,
    required this.addressId,
    required this.items,
    required this.totals,
  });

  factory Cart.fromJson(Map<String, dynamic> json) {
    return Cart(
      cartId: (json['cartId'] ?? '').toString(),
      storeId: (json['storeId'] ?? '').toString(),
      addressId: (json['addressId'] ?? '').toString(),
      items: (json['items'] as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .map(CartItem.fromJson)
          .toList(growable: false),
      totals: json['totals'] is Map<String, dynamic>
          ? CartTotals.fromJson(json['totals'] as Map<String, dynamic>)
          : const CartTotals.empty(),
    );
  }

  const Cart.empty()
    : cartId = '',
      storeId = '',
      addressId = '',
      items = const <CartItem>[],
      totals = const CartTotals.empty();

  final String cartId;
  final String storeId;
  final String addressId;
  final List<CartItem> items;
  final CartTotals totals;

  bool get isEmpty => items.isEmpty;
}

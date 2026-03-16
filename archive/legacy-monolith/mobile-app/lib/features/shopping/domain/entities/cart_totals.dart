class CartTotals {
  const CartTotals({
    required this.subtotal,
    required this.discountAmount,
    required this.shippingAmount,
    required this.totalAmount,
    required this.couponCode,
    required this.appliedPromotionCodes,
    required this.freeShippingApplied,
  });

  factory CartTotals.fromJson(Map<String, dynamic> json) {
    return CartTotals(
      subtotal: _toDouble(json['subtotal']),
      discountAmount: _toDouble(json['discountAmount']),
      shippingAmount: _toDouble(json['shippingAmount']),
      totalAmount: _toDouble(json['totalAmount']),
      couponCode: (json['couponCode'] as String?) ?? '',
      appliedPromotionCodes:
          (json['appliedPromotionCodes'] as List<dynamic>? ?? const [])
              .map((dynamic value) => value.toString())
              .toList(growable: false),
      freeShippingApplied: json['freeShippingApplied'] == true,
    );
  }

  const CartTotals.empty()
    : subtotal = 0,
      discountAmount = 0,
      shippingAmount = 0,
      totalAmount = 0,
      couponCode = '',
      appliedPromotionCodes = const <String>[],
      freeShippingApplied = false;

  final double subtotal;
  final double discountAmount;
  final double shippingAmount;
  final double totalAmount;
  final String couponCode;
  final List<String> appliedPromotionCodes;
  final bool freeShippingApplied;

  static double _toDouble(Object? value) {
    if (value is num) {
      return value.toDouble();
    }
    if (value is String) {
      return double.tryParse(value) ?? 0;
    }
    return 0;
  }
}

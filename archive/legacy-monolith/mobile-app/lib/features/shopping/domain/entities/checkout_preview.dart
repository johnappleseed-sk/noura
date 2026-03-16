import 'cart.dart';

class CheckoutPreview {
  const CheckoutPreview({
    required this.step,
    required this.nextStep,
    required this.message,
    required this.cart,
  });

  factory CheckoutPreview.fromJson(Map<String, dynamic> json) {
    return CheckoutPreview(
      step: (json['step'] as String?) ?? '',
      nextStep: (json['nextStep'] as String?) ?? '',
      message: (json['message'] as String?) ?? '',
      cart: json['cart'] is Map<String, dynamic>
          ? Cart.fromJson(json['cart'] as Map<String, dynamic>)
          : const Cart.empty(),
    );
  }

  final String step;
  final String nextStep;
  final String message;
  final Cart cart;
}

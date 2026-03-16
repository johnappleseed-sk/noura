import '../domain/entities/product_review.dart';

class ProductReviewsState {
  const ProductReviewsState({
    required this.items,
    required this.isSubmitting,
    this.actionMessage,
    this.actionError,
  });

  const ProductReviewsState.initial()
    : items = const <ProductReview>[],
      isSubmitting = false,
      actionMessage = null,
      actionError = null;

  final List<ProductReview> items;
  final bool isSubmitting;
  final String? actionMessage;
  final String? actionError;

  double get averageRating {
    if (items.isEmpty) {
      return 0;
    }
    final total = items.fold<int>(0, (sum, review) => sum + review.rating);
    return total / items.length;
  }

  ProductReviewsState copyWith({
    List<ProductReview>? items,
    bool? isSubmitting,
    Object? actionMessage = _unset,
    Object? actionError = _unset,
  }) {
    return ProductReviewsState(
      items: items ?? this.items,
      isSubmitting: isSubmitting ?? this.isSubmitting,
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

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/error/app_exception.dart';
import '../../../core/error/error_presenter.dart';
import '../data/repositories/commerce_repository_impl.dart';
import '../domain/entities/product_review.dart';
import 'product_reviews_state.dart';

class ProductReviewsController
    extends FamilyAsyncNotifier<ProductReviewsState, String> {
  late String _productId;

  @override
  Future<ProductReviewsState> build(String arg) async {
    _productId = arg.trim();
    if (_productId.isEmpty) {
      return const ProductReviewsState.initial();
    }

    final items = await _loadReviews();
    return ProductReviewsState(items: items, isSubmitting: false);
  }

  Future<void> refresh() async {
    state = const AsyncLoading<ProductReviewsState>();
    state = await AsyncValue.guard<ProductReviewsState>(() async {
      final items = await _loadReviews();
      return ProductReviewsState(items: items, isSubmitting: false);
    });
  }

  Future<bool> submitReview({
    required int rating,
    required String comment,
  }) async {
    final normalizedComment = comment.trim();
    if (_productId.isEmpty) {
      _setActionError('Cannot submit review for an unknown product.');
      return false;
    }
    if (rating < 1 || rating > 5) {
      _setActionError('Rating must be between 1 and 5.');
      return false;
    }
    if (normalizedComment.isEmpty) {
      _setActionError('Review comment is required.');
      return false;
    }

    final current = state.valueOrNull ?? const ProductReviewsState.initial();
    state = AsyncData<ProductReviewsState>(
      current.copyWith(
        isSubmitting: true,
        actionError: null,
        actionMessage: null,
      ),
    );

    try {
      final created = await ref
          .read(commerceRepositoryProvider)
          .addProductReview(
            productId: _productId,
            rating: rating,
            comment: normalizedComment,
          );

      final deduplicated = current.items.where((item) => item.id != created.id);
      final items = <ProductReview>[created, ...deduplicated];

      state = AsyncData<ProductReviewsState>(
        ProductReviewsState(
          items: items,
          isSubmitting: false,
          actionMessage: 'Review submitted.',
        ),
      );
      return true;
    } on AppException catch (error) {
      _setActionError(error.message);
      return false;
    } on Object catch (error) {
      _setActionError(ErrorPresenter.message(error));
      return false;
    }
  }

  void clearMessages() {
    final current = state.valueOrNull;
    if (current == null) {
      return;
    }

    state = AsyncData<ProductReviewsState>(
      current.copyWith(actionError: null, actionMessage: null),
    );
  }

  Future<List<ProductReview>> _loadReviews() {
    return ref
        .read(commerceRepositoryProvider)
        .getProductReviews(productId: _productId);
  }

  void _setActionError(String message) {
    final current = state.valueOrNull ?? const ProductReviewsState.initial();
    state = AsyncData<ProductReviewsState>(
      current.copyWith(
        isSubmitting: false,
        actionError: message,
        actionMessage: null,
      ),
    );
  }
}

final productReviewsControllerProvider =
    AsyncNotifierProvider.family<
      ProductReviewsController,
      ProductReviewsState,
      String
    >(ProductReviewsController.new);

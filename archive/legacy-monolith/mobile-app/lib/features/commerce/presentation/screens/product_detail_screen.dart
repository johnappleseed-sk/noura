import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/utils/form_validators.dart';
import '../../../../core/widgets/app_empty_view.dart';
import '../../../../core/widgets/app_error_view.dart';
import '../../../../core/widgets/app_inline_banner.dart';
import '../../../../core/widgets/app_loading_view.dart';
import '../../application/product_detail_provider.dart';
import '../../application/product_reviews_controller.dart';
import '../../application/product_reviews_state.dart';
import '../../../shopping/application/cart_controller.dart';
import '../../../wishlist/application/wishlist_controller.dart';

class ProductDetailScreen extends ConsumerWidget {
  const ProductDetailScreen({super.key, required this.productId});

  final String productId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final detailState = ref.watch(productDetailProvider(productId));
    final reviewsState = ref.watch(productReviewsControllerProvider(productId));
    final formatter = NumberFormat.currency(symbol: '\$');
    final isWishlisted = ref.watch(wishlistContainsProvider(productId));

    return Scaffold(
      appBar: AppBar(
        title: const Text('Product Detail'),
        actions: [
          IconButton(
            onPressed: () =>
                ref.read(wishlistControllerProvider.notifier).toggle(productId),
            icon: Icon(
              isWishlisted ? Icons.favorite : Icons.favorite_border_outlined,
            ),
          ),
          IconButton(
            onPressed: () => context.go(AppRoutes.cartPath),
            icon: const Icon(Icons.shopping_cart_outlined),
          ),
        ],
      ),
      body: detailState.when(
        loading: () => const AppLoadingView(message: 'Loading product...'),
        error: (Object error, StackTrace stackTrace) => AppErrorView(
          message: error.toString(),
          onRetry: () => ref.invalidate(productDetailProvider(productId)),
        ),
        data: (product) {
          if (product.id.isEmpty) {
            return const AppEmptyView(title: 'Product not found.');
          }
          return ListView(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
            children: [
              AspectRatio(
                aspectRatio: 1.15,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(16),
                  child:
                      product.primaryMedia == null ||
                          product.primaryMedia!.url.isEmpty
                      ? Container(
                          color: Theme.of(
                            context,
                          ).colorScheme.surfaceContainerHighest,
                          alignment: Alignment.center,
                          child: const Icon(Icons.image_not_supported_outlined),
                        )
                      : CachedNetworkImage(
                          imageUrl: product.primaryMedia!.url,
                          fit: BoxFit.cover,
                          placeholder: (BuildContext context, String url) =>
                              const AppLoadingView(),
                          errorWidget:
                              (
                                BuildContext context,
                                String url,
                                Object error,
                              ) => Container(
                                color: Theme.of(
                                  context,
                                ).colorScheme.surfaceContainerHighest,
                                alignment: Alignment.center,
                                child: const Icon(Icons.broken_image_outlined),
                              ),
                        ),
                ),
              ),
              const SizedBox(height: 14),
              Text(
                product.name,
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  _MetaChip(
                    label: product.category.isEmpty
                        ? 'General'
                        : product.category,
                  ),
                  _MetaChip(
                    label: product.brand.isEmpty
                        ? 'Unknown brand'
                        : product.brand,
                  ),
                  _MetaChip(label: product.active ? 'Active' : 'Inactive'),
                  _MetaChip(
                    label: product.allowBackorder
                        ? 'Backorder'
                        : 'In Stock Only',
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                formatter.format(product.price),
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  color: Theme.of(context).colorScheme.primary,
                  fontWeight: FontWeight.w800,
                ),
              ),
              const SizedBox(height: 10),
              Text(
                'Rating ${product.averageRating.toStringAsFixed(1)}'
                ' (${product.reviewCount} reviews) • Stock ${product.totalStock}',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
              if (product.shortDescription.isNotEmpty) ...[
                const SizedBox(height: 14),
                Text(
                  product.shortDescription,
                  style: Theme.of(context).textTheme.bodyLarge,
                ),
              ],
              if (product.longDescription.isNotEmpty) ...[
                const SizedBox(height: 14),
                Text(
                  product.longDescription,
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
              ],
              if (product.attributes.isNotEmpty) ...[
                const SizedBox(height: 18),
                Text(
                  'Attributes',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                const SizedBox(height: 8),
                ...product.attributes.entries.map(
                  (entry) => ListTile(
                    dense: true,
                    contentPadding: EdgeInsets.zero,
                    title: Text(entry.key),
                    subtitle: Text('${entry.value}'),
                  ),
                ),
              ],
              if (product.variants.isNotEmpty) ...[
                const SizedBox(height: 18),
                Text('Variants', style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 8),
                ...product.variants.map(
                  (variant) => Card(
                    child: ListTile(
                      title: Text(
                        [
                          if (variant.color.isNotEmpty) variant.color,
                          if (variant.size.isNotEmpty) variant.size,
                        ].join(' • '),
                      ),
                      subtitle: Text(
                        'SKU: ${variant.sku} • Stock: ${variant.stock}',
                      ),
                      trailing: variant.active
                          ? null
                          : const Icon(Icons.block, color: Colors.red),
                    ),
                  ),
                ),
              ],
              const SizedBox(height: 16),
              FilledButton(
                onPressed: () async {
                  final success = await ref
                      .read(cartControllerProvider.notifier)
                      .addItem(productId: product.id);
                  if (!context.mounted) {
                    return;
                  }
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text(
                        success
                            ? 'Added to cart.'
                            : 'Could not add to cart. Please try again.',
                      ),
                    ),
                  );
                },
                child: const Text('Add to cart'),
              ),
              const SizedBox(height: 18),
              _ReviewsSection(
                reviewsState: reviewsState,
                onWriteReview: () async {
                  ref
                      .read(
                        productReviewsControllerProvider(productId).notifier,
                      )
                      .clearMessages();
                  await showModalBottomSheet<void>(
                    context: context,
                    isScrollControlled: true,
                    builder: (BuildContext context) =>
                        _ReviewComposerSheet(productId: productId),
                  );
                },
                onRetry: () => ref
                    .read(productReviewsControllerProvider(productId).notifier)
                    .refresh(),
                onClearMessages: () => ref
                    .read(productReviewsControllerProvider(productId).notifier)
                    .clearMessages(),
              ),
            ],
          );
        },
      ),
    );
  }
}

class _MetaChip extends StatelessWidget {
  const _MetaChip({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(label, style: Theme.of(context).textTheme.labelMedium),
    );
  }
}

class _ReviewsSection extends StatelessWidget {
  const _ReviewsSection({
    required this.reviewsState,
    required this.onWriteReview,
    required this.onRetry,
    required this.onClearMessages,
  });

  final AsyncValue<ProductReviewsState> reviewsState;
  final VoidCallback onWriteReview;
  final VoidCallback onRetry;
  final VoidCallback onClearMessages;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: reviewsState.when(
          loading: () => const SizedBox(
            height: 110,
            child: AppLoadingView(message: 'Loading reviews...'),
          ),
          error: (Object error, StackTrace stackTrace) => Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Reviews', style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 8),
              Text(
                'Could not load reviews.',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
              const SizedBox(height: 8),
              OutlinedButton(onPressed: onRetry, child: const Text('Retry')),
            ],
          ),
          data: (state) {
            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        'Reviews',
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                    ),
                    FilledButton.tonalIcon(
                      onPressed: state.isSubmitting ? null : onWriteReview,
                      icon: const Icon(Icons.rate_review_outlined),
                      label: const Text('Write review'),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Text(
                  '${state.averageRating.toStringAsFixed(1)} / 5'
                  ' (${state.items.length} reviews)',
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
                if (state.actionError != null) ...[
                  const SizedBox(height: 10),
                  AppInlineBanner(
                    message: state.actionError!,
                    isError: true,
                    onClose: onClearMessages,
                  ),
                ],
                if (state.actionMessage != null) ...[
                  const SizedBox(height: 10),
                  AppInlineBanner(
                    message: state.actionMessage!,
                    isError: false,
                    onClose: onClearMessages,
                  ),
                ],
                const SizedBox(height: 10),
                if (state.items.isEmpty)
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.surfaceContainerLow,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      'No reviews yet. Be the first to share feedback.',
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                  )
                else
                  ...state.items.map(
                    (review) => Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: Theme.of(
                            context,
                          ).colorScheme.surfaceContainerLow,
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Expanded(
                                  child: Text(
                                    review.userName,
                                    style: Theme.of(context)
                                        .textTheme
                                        .titleSmall
                                        ?.copyWith(fontWeight: FontWeight.w700),
                                  ),
                                ),
                                _RatingStars(rating: review.rating),
                              ],
                            ),
                            const SizedBox(height: 6),
                            Text(
                              review.comment.isEmpty
                                  ? 'No comment provided.'
                                  : review.comment,
                              style: Theme.of(context).textTheme.bodyMedium,
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
              ],
            );
          },
        ),
      ),
    );
  }
}

class _RatingStars extends StatelessWidget {
  const _RatingStars({required this.rating});

  final int rating;

  @override
  Widget build(BuildContext context) {
    final normalized = rating.clamp(1, 5);
    final active = Theme.of(context).colorScheme.primary;
    final inactive = Theme.of(context).colorScheme.outline;

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: List<Widget>.generate(5, (index) {
        return Icon(
          index < normalized ? Icons.star : Icons.star_border,
          size: 16,
          color: index < normalized ? active : inactive,
        );
      }),
    );
  }
}

class _ReviewComposerSheet extends ConsumerStatefulWidget {
  const _ReviewComposerSheet({required this.productId});

  final String productId;

  @override
  ConsumerState<_ReviewComposerSheet> createState() =>
      _ReviewComposerSheetState();
}

class _ReviewComposerSheetState extends ConsumerState<_ReviewComposerSheet> {
  final _formKey = GlobalKey<FormState>();
  final TextEditingController _commentController = TextEditingController();
  int _rating = 5;

  @override
  void dispose() {
    _commentController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    final success = await ref
        .read(productReviewsControllerProvider(widget.productId).notifier)
        .submitReview(rating: _rating, comment: _commentController.text);

    if (success && mounted) {
      Navigator.of(context).pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    final reviewsState = ref.watch(
      productReviewsControllerProvider(widget.productId),
    );
    final isSubmitting = reviewsState.valueOrNull?.isSubmitting ?? false;
    final actionError = reviewsState.valueOrNull?.actionError;

    return Padding(
      padding: EdgeInsets.only(
        left: 16,
        right: 16,
        top: 16,
        bottom: MediaQuery.viewInsetsOf(context).bottom + 16,
      ),
      child: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Write a review',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 10),
            Text('Rating', style: Theme.of(context).textTheme.titleSmall),
            const SizedBox(height: 6),
            Row(
              children: List<Widget>.generate(5, (index) {
                final starValue = index + 1;
                return IconButton(
                  onPressed: isSubmitting
                      ? null
                      : () {
                          setState(() {
                            _rating = starValue;
                          });
                        },
                  icon: Icon(
                    starValue <= _rating ? Icons.star : Icons.star_border,
                    color: Theme.of(context).colorScheme.primary,
                  ),
                );
              }),
            ),
            const SizedBox(height: 8),
            TextFormField(
              controller: _commentController,
              enabled: !isSubmitting,
              maxLines: 4,
              maxLength: 500,
              validator: (value) {
                final requiredValidation = FormValidators.requiredField(
                  value,
                  fieldName: 'Comment',
                );
                if (requiredValidation != null) {
                  return requiredValidation;
                }
                if (value!.trim().length < 3) {
                  return 'Comment must be at least 3 characters.';
                }
                return null;
              },
              decoration: const InputDecoration(
                labelText: 'Your feedback',
                hintText: 'Share product quality, value, and experience.',
              ),
            ),
            if (actionError != null) ...[
              const SizedBox(height: 8),
              Text(
                actionError,
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
            ],
            const SizedBox(height: 8),
            Align(
              alignment: Alignment.centerRight,
              child: FilledButton(
                onPressed: isSubmitting ? null : _submit,
                child: Text(isSubmitting ? 'Submitting...' : 'Submit review'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

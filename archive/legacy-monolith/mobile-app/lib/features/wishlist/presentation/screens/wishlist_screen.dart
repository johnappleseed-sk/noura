import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/app_empty_view.dart';
import '../../../../core/widgets/app_error_view.dart';
import '../../../../core/widgets/app_inline_banner.dart';
import '../../../../core/widgets/app_loading_view.dart';
import '../../../commerce/presentation/widgets/commerce_product_tile.dart';
import '../../../shopping/application/cart_controller.dart';
import '../../application/wishlist_controller.dart';
import '../../application/wishlist_state.dart';

class WishlistScreen extends ConsumerWidget {
  const WishlistScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final wishlistAsync = ref.watch(wishlistControllerProvider);
    final formatter = NumberFormat.currency(symbol: '\$');

    return Scaffold(
      appBar: AppBar(title: const Text('Wishlist')),
      body: wishlistAsync.when(
        loading: () => const AppLoadingView(message: 'Loading wishlist...'),
        error: (Object error, StackTrace stackTrace) => AppErrorView(
          message: error.toString(),
          onRetry: () =>
              ref.read(wishlistControllerProvider.notifier).refresh(),
        ),
        data: (WishlistState state) {
          if (state.products.isEmpty) {
            return AppEmptyView(
              title: 'Wishlist is empty.',
              subtitle: 'Save products to view them here.',
              actionLabel: 'Browse products',
              onActionPressed: () => context.go(AppRoutes.productsPath),
            );
          }

          return RefreshIndicator(
            onRefresh: () =>
                ref.read(wishlistControllerProvider.notifier).refresh(),
            child: ListView(
              padding: const EdgeInsets.fromLTRB(12, 10, 12, 20),
              children: [
                if (state.actionError != null) ...[
                  AppInlineBanner(
                    message: state.actionError!,
                    isError: true,
                    onClose: () => ref
                        .read(wishlistControllerProvider.notifier)
                        .clearMessages(),
                  ),
                  const SizedBox(height: 10),
                ],
                if (state.actionMessage != null) ...[
                  AppInlineBanner(
                    message: state.actionMessage!,
                    isError: false,
                    onClose: () => ref
                        .read(wishlistControllerProvider.notifier)
                        .clearMessages(),
                  ),
                  const SizedBox(height: 10),
                ],
                ...state.products.map(
                  (product) => Card(
                    margin: const EdgeInsets.only(bottom: 10),
                    child: Padding(
                      padding: const EdgeInsets.all(10),
                      child: Row(
                        children: [
                          Expanded(
                            child: CommerceProductTile(
                              name: product.name,
                              subtitle: product.category,
                              imageUrl: product.primaryMedia?.url ?? '',
                              priceLabel: formatter.format(product.price),
                              onTap: () => context.go(
                                AppRoutes.productDetailPath.replaceFirst(
                                  ':productId',
                                  product.id,
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Column(
                            children: [
                              IconButton(
                                onPressed: state.isMutating
                                    ? null
                                    : () => ref
                                          .read(
                                            wishlistControllerProvider.notifier,
                                          )
                                          .remove(product.id),
                                icon: const Icon(Icons.delete_outline),
                              ),
                              IconButton(
                                onPressed: () => ref
                                    .read(cartControllerProvider.notifier)
                                    .addItem(productId: product.id),
                                icon: const Icon(Icons.shopping_cart_outlined),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

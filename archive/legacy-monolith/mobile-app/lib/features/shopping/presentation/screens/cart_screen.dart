import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/app_empty_view.dart';
import '../../../../core/widgets/app_error_view.dart';
import '../../../../core/widgets/app_inline_banner.dart';
import '../../../../core/widgets/app_loading_view.dart';
import '../../application/cart_controller.dart';
import '../../application/cart_state.dart';

class CartScreen extends ConsumerStatefulWidget {
  const CartScreen({super.key});

  @override
  ConsumerState<CartScreen> createState() => _CartScreenState();
}

class _CartScreenState extends ConsumerState<CartScreen> {
  final TextEditingController _couponController = TextEditingController();

  @override
  void dispose() {
    _couponController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final cartAsync = ref.watch(cartControllerProvider);
    final formatter = NumberFormat.currency(symbol: '\$');

    return Scaffold(
      appBar: AppBar(
        title: const Text('Cart'),
        actions: [
          IconButton(
            onPressed: () => context.go(AppRoutes.wishlistPath),
            icon: const Icon(Icons.favorite_outline),
          ),
        ],
      ),
      body: cartAsync.when(
        loading: () => const AppLoadingView(message: 'Loading cart...'),
        error: (Object error, StackTrace stackTrace) => AppErrorView(
          message: error.toString(),
          onRetry: () => ref.read(cartControllerProvider.notifier).refresh(),
        ),
        data: (CartState state) {
          if (_couponController.text != state.cart.totals.couponCode) {
            _couponController.text = state.cart.totals.couponCode;
          }
          if (state.cart.isEmpty) {
            return AppEmptyView(
              title: 'Your cart is empty.',
              subtitle: 'Browse products and add items to continue checkout.',
              actionLabel: 'Go shopping',
              onActionPressed: () => context.go(AppRoutes.homePath),
            );
          }

          return RefreshIndicator(
            onRefresh: () =>
                ref.read(cartControllerProvider.notifier).refresh(),
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
              children: [
                if (state.actionError != null) ...[
                  AppInlineBanner(
                    message: state.actionError!,
                    isError: true,
                    onClose: () => ref
                        .read(cartControllerProvider.notifier)
                        .clearMessages(),
                  ),
                  const SizedBox(height: 10),
                ],
                if (state.actionMessage != null) ...[
                  AppInlineBanner(
                    message: state.actionMessage!,
                    isError: false,
                    onClose: () => ref
                        .read(cartControllerProvider.notifier)
                        .clearMessages(),
                  ),
                  const SizedBox(height: 10),
                ],
                ...state.cart.items.map(
                  (item) => Card(
                    margin: const EdgeInsets.only(bottom: 10),
                    child: Padding(
                      padding: const EdgeInsets.all(12),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            item.productName,
                            style: Theme.of(context).textTheme.titleMedium,
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'Unit ${formatter.format(item.unitPrice)}',
                            style: Theme.of(context).textTheme.bodyMedium,
                          ),
                          const SizedBox(height: 10),
                          Row(
                            children: [
                              IconButton(
                                onPressed: state.isMutating
                                    ? null
                                    : () {
                                        if (item.quantity <= 1) {
                                          ref
                                              .read(
                                                cartControllerProvider.notifier,
                                              )
                                              .removeItem(cartItemId: item.id);
                                        } else {
                                          ref
                                              .read(
                                                cartControllerProvider.notifier,
                                              )
                                              .updateQuantity(
                                                cartItemId: item.id,
                                                quantity: item.quantity - 1,
                                              );
                                        }
                                      },
                                icon: const Icon(Icons.remove_circle_outline),
                              ),
                              Text(
                                '${item.quantity}',
                                style: Theme.of(context).textTheme.titleMedium,
                              ),
                              IconButton(
                                onPressed: state.isMutating
                                    ? null
                                    : () => ref
                                          .read(cartControllerProvider.notifier)
                                          .updateQuantity(
                                            cartItemId: item.id,
                                            quantity: item.quantity + 1,
                                          ),
                                icon: const Icon(Icons.add_circle_outline),
                              ),
                              const Spacer(),
                              Text(
                                formatter.format(item.lineTotal),
                                style: Theme.of(context).textTheme.titleMedium
                                    ?.copyWith(fontWeight: FontWeight.w700),
                              ),
                              IconButton(
                                onPressed: state.isMutating
                                    ? null
                                    : () => ref
                                          .read(cartControllerProvider.notifier)
                                          .removeItem(cartItemId: item.id),
                                icon: const Icon(Icons.delete_outline),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: Column(
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: TextField(
                                controller: _couponController,
                                decoration: const InputDecoration(
                                  hintText: 'Coupon code',
                                  isDense: true,
                                ),
                              ),
                            ),
                            const SizedBox(width: 10),
                            FilledButton.tonal(
                              onPressed: state.isMutating
                                  ? null
                                  : () {
                                      final code = _couponController.text
                                          .trim();
                                      if (code.isEmpty) {
                                        return;
                                      }
                                      ref
                                          .read(cartControllerProvider.notifier)
                                          .applyCoupon(code);
                                    },
                              child: const Text('Apply'),
                            ),
                          ],
                        ),
                        const SizedBox(height: 14),
                        _PriceRow(
                          label: 'Subtotal',
                          value: formatter.format(state.cart.totals.subtotal),
                        ),
                        _PriceRow(
                          label: 'Discount',
                          value: formatter.format(
                            state.cart.totals.discountAmount,
                          ),
                        ),
                        _PriceRow(
                          label: 'Shipping',
                          value: formatter.format(
                            state.cart.totals.shippingAmount,
                          ),
                        ),
                        const Divider(),
                        _PriceRow(
                          label: 'Total',
                          value: formatter.format(
                            state.cart.totals.totalAmount,
                          ),
                          emphasize: true,
                        ),
                        const SizedBox(height: 12),
                        SizedBox(
                          width: double.infinity,
                          child: FilledButton(
                            onPressed: state.isMutating
                                ? null
                                : () => context.go(AppRoutes.checkoutPath),
                            child: state.isMutating
                                ? const SizedBox(
                                    height: 18,
                                    width: 18,
                                    child: CircularProgressIndicator(
                                      strokeWidth: 2,
                                    ),
                                  )
                                : const Text('Proceed to checkout'),
                          ),
                        ),
                        const SizedBox(height: 8),
                        TextButton(
                          onPressed: state.isMutating
                              ? null
                              : () => ref
                                    .read(cartControllerProvider.notifier)
                                    .clearCart(),
                          child: const Text('Clear cart'),
                        ),
                      ],
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

class _PriceRow extends StatelessWidget {
  const _PriceRow({
    required this.label,
    required this.value,
    this.emphasize = false,
  });

  final String label;
  final String value;
  final bool emphasize;

  @override
  Widget build(BuildContext context) {
    final style = emphasize
        ? Theme.of(
            context,
          ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700)
        : Theme.of(context).textTheme.bodyMedium;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        children: [
          Text(label, style: style),
          const Spacer(),
          Text(value, style: style),
        ],
      ),
    );
  }
}

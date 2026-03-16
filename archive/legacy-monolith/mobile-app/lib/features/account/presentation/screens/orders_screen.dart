import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/app_empty_view.dart';
import '../../../../core/widgets/app_error_view.dart';
import '../../../../core/widgets/app_inline_banner.dart';
import '../../../../core/widgets/app_loading_view.dart';
import '../../application/orders_controller.dart';
import '../../application/orders_state.dart';

class OrdersScreen extends ConsumerWidget {
  const OrdersScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ordersAsync = ref.watch(ordersControllerProvider);
    final formatter = NumberFormat.currency(symbol: '\$');
    final dateFormatter = DateFormat('yyyy-MM-dd HH:mm');

    return Scaffold(
      appBar: AppBar(title: const Text('My Orders')),
      body: ordersAsync.when(
        loading: () => const AppLoadingView(message: 'Loading orders...'),
        error: (Object error, StackTrace stackTrace) => AppErrorView(
          message: error.toString(),
          onRetry: () => ref.read(ordersControllerProvider.notifier).refresh(),
        ),
        data: (OrdersState state) {
          if (state.orders.isEmpty) {
            return AppEmptyView(
              title: 'No orders yet.',
              subtitle: 'Completed checkouts will appear here.',
              actionLabel: 'Start shopping',
              onActionPressed: () => context.go(AppRoutes.homePath),
            );
          }

          return RefreshIndicator(
            onRefresh: () =>
                ref.read(ordersControllerProvider.notifier).refresh(),
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
              children: [
                if (state.error != null) ...[
                  AppInlineBanner(
                    message: state.error!,
                    isError: true,
                    onClose: () => ref
                        .read(ordersControllerProvider.notifier)
                        .clearMessages(),
                  ),
                  const SizedBox(height: 10),
                ],
                if (state.message != null) ...[
                  AppInlineBanner(
                    message: state.message!,
                    isError: false,
                    onClose: () => ref
                        .read(ordersControllerProvider.notifier)
                        .clearMessages(),
                  ),
                  const SizedBox(height: 10),
                ],
                ...state.orders.map(
                  (order) => Card(
                    margin: const EdgeInsets.only(bottom: 10),
                    child: ListTile(
                      onTap: () => context.go(
                        AppRoutes.orderDetailPath.replaceFirst(
                          ':orderId',
                          order.id,
                        ),
                      ),
                      title: Text('#${order.id}'),
                      subtitle: Text(
                        [
                          order.status,
                          if (order.createdAt != null)
                            dateFormatter.format(order.createdAt!.toLocal()),
                        ].join(' • '),
                      ),
                      trailing: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Text(
                            formatter.format(order.totalAmount),
                            style: Theme.of(context).textTheme.titleSmall,
                          ),
                          TextButton(
                            onPressed: state.isMutating
                                ? null
                                : () => ref
                                      .read(ordersControllerProvider.notifier)
                                      .quickReorder(order.id),
                            child: const Text('Quick reorder'),
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

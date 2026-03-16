import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../../core/widgets/app_empty_view.dart';
import '../../../../core/widgets/app_error_view.dart';
import '../../../../core/widgets/app_loading_view.dart';
import '../../application/order_detail_provider.dart';

class OrderDetailScreen extends ConsumerWidget {
  const OrderDetailScreen({super.key, required this.orderId});

  final String orderId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final orderAsync = ref.watch(orderDetailProvider(orderId));
    final formatter = NumberFormat.currency(symbol: '\$');
    final dateFormatter = DateFormat('yyyy-MM-dd HH:mm');

    return Scaffold(
      appBar: AppBar(title: const Text('Order Detail')),
      body: orderAsync.when(
        loading: () => const AppLoadingView(message: 'Loading order detail...'),
        error: (Object error, StackTrace stackTrace) => AppErrorView(
          message: error.toString(),
          onRetry: () => ref.invalidate(orderDetailProvider(orderId)),
        ),
        data: (state) {
          final order = state.order;
          if (order.id.isEmpty) {
            return const AppEmptyView(title: 'Order not found.');
          }

          return ListView(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
            children: [
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Order #${order.id}',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 6),
                      Text('Status: ${order.status}'),
                      Text('Fulfillment: ${order.fulfillmentMethod}'),
                      if (order.createdAt != null)
                        Text(
                          'Created: ${dateFormatter.format(order.createdAt!.toLocal())}',
                        ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 10),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Items',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 8),
                      ...order.items.map(
                        (item) => Padding(
                          padding: const EdgeInsets.only(bottom: 6),
                          child: Row(
                            children: [
                              Expanded(
                                child: Text(
                                  '${item.productName} x${item.quantity}',
                                ),
                              ),
                              Text(formatter.format(item.lineTotal)),
                            ],
                          ),
                        ),
                      ),
                      const Divider(),
                      _AmountRow(
                        label: 'Subtotal',
                        value: formatter.format(order.subtotal),
                      ),
                      _AmountRow(
                        label: 'Discount',
                        value: formatter.format(order.discountAmount),
                      ),
                      _AmountRow(
                        label: 'Shipping',
                        value: formatter.format(order.shippingAmount),
                      ),
                      _AmountRow(
                        label: 'Total',
                        value: formatter.format(order.totalAmount),
                        emphasize: true,
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 10),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Timeline',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 8),
                      if (state.timeline.isEmpty)
                        const Text('No timeline events yet.')
                      else
                        ...state.timeline.map(
                          (event) => ListTile(
                            contentPadding: EdgeInsets.zero,
                            title: Text(event.status),
                            subtitle: Text(
                              [
                                if (event.actor.trim().isNotEmpty) event.actor,
                                if (event.note.trim().isNotEmpty) event.note,
                                if (event.createdAt != null)
                                  dateFormatter.format(
                                    event.createdAt!.toLocal(),
                                  ),
                              ].join(' • '),
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

class _AmountRow extends StatelessWidget {
  const _AmountRow({
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

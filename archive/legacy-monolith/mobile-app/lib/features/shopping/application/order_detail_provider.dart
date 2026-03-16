import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/repositories/shopping_repository_impl.dart';
import '../domain/entities/order.dart';

class OrderDetailState {
  const OrderDetailState({required this.order, required this.timeline});

  final Order order;
  final List<OrderTimelineEvent> timeline;
}

final orderDetailProvider = FutureProvider.family<OrderDetailState, String>((
  ref,
  orderId,
) async {
  final repository = ref.read(shoppingRepositoryProvider);
  final results = await Future.wait<Object>([
    repository.getOrderById(orderId: orderId),
    repository.getOrderTimeline(orderId: orderId),
  ]);
  return OrderDetailState(
    order: results[0] as Order,
    timeline: results[1] as List<OrderTimelineEvent>,
  );
});

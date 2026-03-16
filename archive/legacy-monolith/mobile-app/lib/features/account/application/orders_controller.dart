import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/error/app_exception.dart';
import '../../../core/error/error_presenter.dart';
import '../data/repositories/account_repository_impl.dart';
import '../../shopping/domain/entities/order.dart';
import 'orders_state.dart';

class OrdersController extends AsyncNotifier<OrdersState> {
  @override
  Future<OrdersState> build() async {
    final orders = await _loadOrders();
    return OrdersState(orders: orders, isMutating: false);
  }

  Future<void> refresh() async {
    state = const AsyncLoading<OrdersState>();
    state = await AsyncValue.guard<OrdersState>(() async {
      final orders = await _loadOrders();
      return OrdersState(orders: orders, isMutating: false);
    });
  }

  Future<bool> quickReorder(String orderId) async {
    final current = state.valueOrNull;
    if (current == null) {
      return false;
    }

    state = AsyncData<OrdersState>(
      current.copyWith(isMutating: true, error: null, message: null),
    );

    try {
      await ref.read(accountRepositoryProvider).quickReorder(orderId: orderId);
      final orders = await _loadOrders();
      state = AsyncData<OrdersState>(
        OrdersState(
          orders: orders,
          isMutating: false,
          message: 'Quick reorder completed.',
        ),
      );
      return true;
    } on AppException catch (error) {
      state = AsyncData<OrdersState>(
        current.copyWith(
          isMutating: false,
          error: error.message,
          message: null,
        ),
      );
      return false;
    } on Object catch (error) {
      state = AsyncData<OrdersState>(
        current.copyWith(
          isMutating: false,
          error: ErrorPresenter.message(error),
          message: null,
        ),
      );
      return false;
    }
  }

  void clearMessages() {
    final current = state.valueOrNull;
    if (current == null) {
      return;
    }
    state = AsyncData<OrdersState>(
      current.copyWith(error: null, message: null),
    );
  }

  Future<List<Order>> _loadOrders() async {
    final orders = await ref.read(accountRepositoryProvider).getOrderHistory();
    final sorted = orders.toList(growable: false)
      ..sort((a, b) {
        final aDate = a.createdAt?.millisecondsSinceEpoch ?? 0;
        final bDate = b.createdAt?.millisecondsSinceEpoch ?? 0;
        return bDate.compareTo(aDate);
      });
    return sorted;
  }
}

final ordersControllerProvider =
    AsyncNotifierProvider<OrdersController, OrdersState>(OrdersController.new);

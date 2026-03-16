import '../../shopping/domain/entities/order.dart';

class OrdersState {
  const OrdersState({
    required this.orders,
    required this.isMutating,
    this.message,
    this.error,
  });

  const OrdersState.initial()
    : orders = const <Order>[],
      isMutating = false,
      message = null,
      error = null;

  final List<Order> orders;
  final bool isMutating;
  final String? message;
  final String? error;

  OrdersState copyWith({
    List<Order>? orders,
    bool? isMutating,
    Object? message = _unset,
    Object? error = _unset,
  }) {
    return OrdersState(
      orders: orders ?? this.orders,
      isMutating: isMutating ?? this.isMutating,
      message: identical(message, _unset) ? this.message : message as String?,
      error: identical(error, _unset) ? this.error : error as String?,
    );
  }

  static const Object _unset = Object();
}

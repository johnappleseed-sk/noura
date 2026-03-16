import '../domain/entities/payment_method.dart';

class PaymentMethodsState {
  const PaymentMethodsState({
    required this.items,
    required this.isMutating,
    this.actionMessage,
    this.actionError,
  });

  const PaymentMethodsState.initial()
    : items = const <PaymentMethod>[],
      isMutating = false,
      actionMessage = null,
      actionError = null;

  final List<PaymentMethod> items;
  final bool isMutating;
  final String? actionMessage;
  final String? actionError;

  PaymentMethodsState copyWith({
    List<PaymentMethod>? items,
    bool? isMutating,
    Object? actionMessage = _unset,
    Object? actionError = _unset,
  }) {
    return PaymentMethodsState(
      items: items ?? this.items,
      isMutating: isMutating ?? this.isMutating,
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

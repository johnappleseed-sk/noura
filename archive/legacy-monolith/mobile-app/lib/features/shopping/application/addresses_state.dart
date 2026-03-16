import '../domain/entities/address.dart';

class AddressesState {
  const AddressesState({
    required this.items,
    required this.isMutating,
    this.actionMessage,
    this.actionError,
  });

  const AddressesState.initial()
    : items = const <Address>[],
      isMutating = false,
      actionMessage = null,
      actionError = null;

  final List<Address> items;
  final bool isMutating;
  final String? actionMessage;
  final String? actionError;

  AddressesState copyWith({
    List<Address>? items,
    bool? isMutating,
    Object? actionMessage = _unset,
    Object? actionError = _unset,
  }) {
    return AddressesState(
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

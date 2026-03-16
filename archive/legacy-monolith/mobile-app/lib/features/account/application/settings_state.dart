import '../domain/entities/runtime_features.dart';
import '../domain/entities/store_location.dart';

class SettingsState {
  const SettingsState({
    required this.runtimeFeatures,
    required this.stores,
    required this.preferredStoreId,
    required this.isMutating,
    this.message,
    this.error,
  });

  final RuntimeFeatures runtimeFeatures;
  final List<StoreLocation> stores;
  final String preferredStoreId;
  final bool isMutating;
  final String? message;
  final String? error;

  SettingsState copyWith({
    RuntimeFeatures? runtimeFeatures,
    List<StoreLocation>? stores,
    String? preferredStoreId,
    bool? isMutating,
    Object? message = _unset,
    Object? error = _unset,
  }) {
    return SettingsState(
      runtimeFeatures: runtimeFeatures ?? this.runtimeFeatures,
      stores: stores ?? this.stores,
      preferredStoreId: preferredStoreId ?? this.preferredStoreId,
      isMutating: isMutating ?? this.isMutating,
      message: identical(message, _unset) ? this.message : message as String?,
      error: identical(error, _unset) ? this.error : error as String?,
    );
  }

  static const Object _unset = Object();
}

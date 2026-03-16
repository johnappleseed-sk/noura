import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/error/app_exception.dart';
import '../../../core/error/error_presenter.dart';
import '../../auth/application/auth_session_controller.dart';
import '../domain/entities/runtime_features.dart';
import '../domain/entities/store_location.dart';
import '../data/repositories/account_repository_impl.dart';
import 'settings_state.dart';

class SettingsController extends AsyncNotifier<SettingsState> {
  @override
  Future<SettingsState> build() async {
    return _load();
  }

  Future<void> refresh() async {
    state = const AsyncLoading<SettingsState>();
    state = await AsyncValue.guard<SettingsState>(_load);
  }

  Future<bool> setPreferredStore(String storeId) async {
    final current = state.valueOrNull;
    if (current == null || storeId.isEmpty) {
      return false;
    }

    state = AsyncData<SettingsState>(
      current.copyWith(isMutating: true, error: null, message: null),
    );

    try {
      await ref
          .read(accountRepositoryProvider)
          .setPreferredStore(storeId: storeId);
      await ref.read(authSessionControllerProvider.notifier).refreshProfile();

      state = AsyncData<SettingsState>(
        current.copyWith(
          preferredStoreId: storeId,
          isMutating: false,
          message: 'Preferred store updated.',
          error: null,
        ),
      );
      return true;
    } on AppException catch (error) {
      state = AsyncData<SettingsState>(
        current.copyWith(
          isMutating: false,
          error: error.message,
          message: null,
        ),
      );
      return false;
    } on Object catch (error) {
      state = AsyncData<SettingsState>(
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
    state = AsyncData<SettingsState>(
      current.copyWith(error: null, message: null),
    );
  }

  Future<SettingsState> _load() async {
    final sessionState = ref.read(authSessionControllerProvider).valueOrNull;
    final preferredStoreId = sessionState?.profile?.preferredStoreId ?? '';

    final results = await Future.wait<Object>([
      ref.read(accountRepositoryProvider).getRuntimeFeatures(),
      ref.read(accountRepositoryProvider).getStores(size: 40),
    ]);

    final runtimeFeatures = results[0] as RuntimeFeatures;
    final stores = results[1] as List<StoreLocation>;

    return SettingsState(
      runtimeFeatures: runtimeFeatures,
      stores: stores,
      preferredStoreId: preferredStoreId,
      isMutating: false,
    );
  }
}

final settingsControllerProvider =
    AsyncNotifierProvider<SettingsController, SettingsState>(
      SettingsController.new,
    );

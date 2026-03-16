import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/error/app_exception.dart';
import '../../../core/error/error_presenter.dart';
import '../../auth/application/auth_session_controller.dart';
import '../../auth/data/repositories/auth_repository_impl.dart';
import '../../auth/domain/entities/user_profile.dart';
import 'profile_state.dart';

class ProfileController extends AsyncNotifier<ProfileState> {
  @override
  Future<ProfileState> build() async {
    final profile = await _loadProfile();
    return ProfileState(profile: profile, isSaving: false);
  }

  Future<void> refresh() async {
    state = const AsyncLoading<ProfileState>();
    state = await AsyncValue.guard<ProfileState>(() async {
      final profile = await _loadProfile();
      return ProfileState(profile: profile, isSaving: false);
    });
  }

  Future<bool> updateProfile({
    required String fullName,
    required String phone,
  }) async {
    final current = state.valueOrNull;
    if (current == null) {
      return false;
    }
    state = AsyncData<ProfileState>(
      current.copyWith(isSaving: true, error: null, message: null),
    );

    try {
      final updatedProfile = await ref
          .read(authRepositoryProvider)
          .updateProfile(fullName: fullName, phone: phone);
      await ref.read(authSessionControllerProvider.notifier).refreshProfile();
      state = AsyncData<ProfileState>(
        current.copyWith(
          profile: updatedProfile,
          isSaving: false,
          message: 'Profile updated successfully.',
          error: null,
        ),
      );
      return true;
    } on AppException catch (error) {
      state = AsyncData<ProfileState>(
        current.copyWith(isSaving: false, error: error.message, message: null),
      );
      return false;
    } on Object catch (error) {
      state = AsyncData<ProfileState>(
        current.copyWith(
          isSaving: false,
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
    state = AsyncData<ProfileState>(
      current.copyWith(error: null, message: null),
    );
  }

  Future<UserProfile> _loadProfile() async {
    final sessionProfile = ref
        .read(authSessionControllerProvider)
        .valueOrNull
        ?.profile;
    if (sessionProfile != null) {
      return sessionProfile;
    }
    return ref.read(authRepositoryProvider).getProfile();
  }
}

final profileControllerProvider =
    AsyncNotifierProvider<ProfileController, ProfileState>(
      ProfileController.new,
    );

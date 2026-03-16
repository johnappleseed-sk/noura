import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/auth/session_event_bus.dart';
import '../../../core/error/app_exception.dart';
import '../../../core/providers/core_providers.dart';
import '../data/repositories/auth_repository_impl.dart';
import '../domain/entities/auth_session.dart';
import '../domain/entities/user_profile.dart';
import 'auth_session_state.dart';

class AuthSessionController extends AsyncNotifier<AuthSessionState> {
  @override
  Future<AuthSessionState> build() async {
    final sessionEventBus = ref.read(sessionEventBusProvider);
    final subscription = sessionEventBus.stream.listen((event) {
      if (event == SessionEvent.expired) {
        state = const AsyncData<AuthSessionState>(
          AuthSessionState.unauthenticated(),
        );
      }
    });
    ref.onDispose(subscription.cancel);

    final repository = ref.read(authRepositoryProvider);
    final hasSession = await repository.hasSession();
    if (!hasSession) {
      return const AuthSessionState.unauthenticated();
    }

    try {
      final profile = await repository.getProfile();
      return AuthSessionState.authenticated(profile: profile);
    } on UnauthorizedAppException {
      await repository.logout();
      return const AuthSessionState.unauthenticated();
    } on AppException {
      return const AuthSessionState.unauthenticated();
    }
  }

  Future<void> completeAuthentication(AuthSession session) async {
    state = const AsyncLoading<AuthSessionState>();
    state = await AsyncValue.guard<AuthSessionState>(() async {
      final repository = ref.read(authRepositoryProvider);

      try {
        final profile = await repository.getProfile();
        return AuthSessionState.authenticated(profile: profile);
      } on AppException {
        // Fallback to auth payload if profile endpoint temporarily fails.
        final fallbackProfile = UserProfile.fromSession(session);
        return AuthSessionState.authenticated(profile: fallbackProfile);
      }
    });
  }

  Future<void> refreshProfile() async {
    state = const AsyncLoading<AuthSessionState>();
    state = await AsyncValue.guard<AuthSessionState>(() async {
      final repository = ref.read(authRepositoryProvider);
      final profile = await repository.getProfile();
      return AuthSessionState.authenticated(profile: profile);
    });
  }

  Future<void> markLoggedOut() async {
    state = const AsyncData<AuthSessionState>(
      AuthSessionState.unauthenticated(),
    );
  }
}

final authSessionControllerProvider =
    AsyncNotifierProvider<AuthSessionController, AuthSessionState>(
      AuthSessionController.new,
    );

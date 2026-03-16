import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/repositories/auth_repository_impl.dart';
import '../domain/repositories/auth_repository.dart';
import 'auth_session_controller.dart';

class AuthController extends StateNotifier<AsyncValue<void>> {
  AuthController({required AuthRepository authRepository, required Ref ref})
    : _authRepository = authRepository,
      _ref = ref,
      super(const AsyncData<void>(null));

  final AuthRepository _authRepository;
  final Ref _ref;

  Future<bool> login({required String email, required String password}) async {
    state = const AsyncLoading<void>();
    state = await AsyncValue.guard<void>(() async {
      final session = await _authRepository.login(
        email: email,
        password: password,
      );
      await _ref
          .read(authSessionControllerProvider.notifier)
          .completeAuthentication(session);
    });
    return !state.hasError;
  }

  Future<bool> register({
    required String fullName,
    required String email,
    required String password,
  }) async {
    state = const AsyncLoading<void>();
    state = await AsyncValue.guard<void>(() async {
      final session = await _authRepository.register(
        fullName: fullName,
        email: email,
        password: password,
      );
      await _ref
          .read(authSessionControllerProvider.notifier)
          .completeAuthentication(session);
    });
    return !state.hasError;
  }

  Future<bool> requestPasswordReset({required String email}) async {
    state = const AsyncLoading<void>();
    state = await AsyncValue.guard<void>(() async {
      await _authRepository.requestPasswordReset(email: email);
    });
    return !state.hasError;
  }

  Future<bool> confirmPasswordReset({
    required String token,
    required String newPassword,
  }) async {
    state = const AsyncLoading<void>();
    state = await AsyncValue.guard<void>(() async {
      await _authRepository.confirmPasswordReset(
        token: token,
        newPassword: newPassword,
      );
    });
    return !state.hasError;
  }

  Future<void> logout() async {
    await _authRepository.logout();
    await _ref.read(authSessionControllerProvider.notifier).markLoggedOut();
    state = const AsyncData<void>(null);
  }
}

final authControllerProvider =
    StateNotifierProvider<AuthController, AsyncValue<void>>((ref) {
      final authRepository = ref.watch(authRepositoryProvider);
      return AuthController(authRepository: authRepository, ref: ref);
    });

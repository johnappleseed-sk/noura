import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:noura/core/auth/session_event_bus.dart';
import 'package:noura/core/providers/core_providers.dart';
import 'package:noura/features/auth/application/auth_session_controller.dart';
import 'package:noura/features/auth/application/auth_session_state.dart';
import 'package:noura/features/auth/data/repositories/auth_repository_impl.dart';
import 'package:noura/features/auth/domain/entities/auth_session.dart';
import 'package:noura/features/auth/domain/entities/user_profile.dart';
import 'package:noura/features/auth/domain/repositories/auth_repository.dart';

class FakeAuthRepository implements AuthRepository {
  bool sessionExists = false;
  UserProfile? profile;
  AuthSession? loginSession;
  int getProfileCalls = 0;

  @override
  Future<void> confirmPasswordReset({
    required String token,
    required String newPassword,
  }) async {}

  @override
  Future<UserProfile> getProfile() async {
    getProfileCalls++;
    if (profile == null) {
      throw StateError('Profile is not configured');
    }
    return profile!;
  }

  @override
  Future<bool> hasSession() async => sessionExists;

  @override
  Future<AuthSession> login({
    required String email,
    required String password,
  }) async {
    if (loginSession == null) {
      throw StateError('Login session is not configured');
    }
    return loginSession!;
  }

  @override
  Future<void> logout() async {
    sessionExists = false;
  }

  @override
  Future<void> requestPasswordReset({required String email}) async {}

  @override
  Future<AuthSession> register({
    required String fullName,
    required String email,
    required String password,
  }) async {
    if (loginSession == null) {
      throw StateError('Register session is not configured');
    }
    return loginSession!;
  }

  @override
  Future<UserProfile> updateProfile({
    required String fullName,
    required String phone,
  }) async {
    final current =
        profile ??
        const UserProfile(
          id: '',
          fullName: '',
          email: '',
          phone: '',
          roles: <String>{},
          enabled: true,
          preferredStoreId: '',
        );
    profile = current.copyWith(fullName: fullName, phone: phone);
    return profile!;
  }
}

void main() {
  group('AuthSessionController', () {
    late FakeAuthRepository fakeRepository;

    setUp(() {
      fakeRepository = FakeAuthRepository();
    });

    test(
      'build returns unauthenticated when no stored session exists',
      () async {
        fakeRepository.sessionExists = false;

        final container = ProviderContainer(
          overrides: [authRepositoryProvider.overrideWithValue(fakeRepository)],
        );
        addTearDown(container.dispose);

        final state = await container.read(
          authSessionControllerProvider.future,
        );

        expect(state.isAuthenticated, isFalse);
        expect(state.profile, isNull);
      },
    );

    test(
      'build returns authenticated with profile when session exists',
      () async {
        fakeRepository.sessionExists = true;
        fakeRepository.profile = const UserProfile(
          id: '1',
          fullName: 'Session User',
          email: 'session@noura.com',
          phone: '1234',
          roles: <String>{'CUSTOMER'},
          enabled: true,
          preferredStoreId: 'store-1',
        );

        final container = ProviderContainer(
          overrides: [authRepositoryProvider.overrideWithValue(fakeRepository)],
        );
        addTearDown(container.dispose);

        final state = await container.read(
          authSessionControllerProvider.future,
        );

        expect(state.isAuthenticated, isTrue);
        expect(state.profile?.email, 'session@noura.com');
        expect(fakeRepository.getProfileCalls, 1);
      },
    );

    test('completeAuthentication refreshes profile after login', () async {
      fakeRepository.sessionExists = true;
      fakeRepository.profile = const UserProfile(
        id: '2',
        fullName: 'Authenticated User',
        email: 'auth@noura.com',
        phone: '5678',
        roles: <String>{'CUSTOMER'},
        enabled: true,
        preferredStoreId: 'store-2',
      );

      final container = ProviderContainer(
        overrides: [authRepositoryProvider.overrideWithValue(fakeRepository)],
      );
      addTearDown(container.dispose);

      // Ensure provider is initialized.
      await container.read(authSessionControllerProvider.future);

      await container
          .read(authSessionControllerProvider.notifier)
          .completeAuthentication(
            const AuthSession(
              userId: '2',
              email: 'auth@noura.com',
              fullName: 'Authenticated User',
              roles: <String>{'CUSTOMER'},
              accessToken: 'a',
              refreshToken: 'r',
            ),
          );

      final AsyncValue<AuthSessionState> current = container.read(
        authSessionControllerProvider,
      );
      expect(current.value?.isAuthenticated, isTrue);
      expect(current.value?.profile?.fullName, 'Authenticated User');
    });

    test('session expiration event forces unauthenticated state', () async {
      fakeRepository.sessionExists = true;
      fakeRepository.profile = const UserProfile(
        id: '3',
        fullName: 'Session Expired User',
        email: 'expired@noura.com',
        phone: '0000',
        roles: <String>{'CUSTOMER'},
        enabled: true,
        preferredStoreId: 'store-3',
      );

      final container = ProviderContainer(
        overrides: [authRepositoryProvider.overrideWithValue(fakeRepository)],
      );
      addTearDown(container.dispose);

      await container.read(authSessionControllerProvider.future);
      expect(
        container.read(authSessionControllerProvider).value?.isAuthenticated,
        isTrue,
      );

      container.read(sessionEventBusProvider).emit(SessionEvent.expired);
      await Future<void>.delayed(Duration.zero);

      expect(
        container.read(authSessionControllerProvider).value?.isAuthenticated,
        isFalse,
      );
    });
  });
}

import '../entities/auth_session.dart';
import '../entities/user_profile.dart';

abstract class AuthRepository {
  Future<AuthSession> login({required String email, required String password});

  Future<AuthSession> register({
    required String fullName,
    required String email,
    required String password,
  });

  Future<void> requestPasswordReset({required String email});

  Future<void> confirmPasswordReset({
    required String token,
    required String newPassword,
  });

  Future<UserProfile> getProfile();

  Future<UserProfile> updateProfile({
    required String fullName,
    required String phone,
  });

  Future<bool> hasSession();

  Future<void> logout();
}

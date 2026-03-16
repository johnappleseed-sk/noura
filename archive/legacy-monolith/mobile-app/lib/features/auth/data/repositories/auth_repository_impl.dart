import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/error/app_exception.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/network/api_endpoints.dart';
import '../../../../core/providers/core_providers.dart';
import '../../../../core/storage/token_storage.dart';
import '../dto/login_request_dto.dart';
import '../dto/password_reset_confirm_request_dto.dart';
import '../dto/password_reset_request_dto.dart';
import '../dto/register_request_dto.dart';
import '../dto/update_profile_request_dto.dart';
import '../../domain/entities/auth_session.dart';
import '../../domain/entities/user_profile.dart';
import '../../domain/repositories/auth_repository.dart';

class AuthRepositoryImpl implements AuthRepository {
  AuthRepositoryImpl({
    required ApiClient apiClient,
    required TokenStorage tokenStorage,
  }) : _apiClient = apiClient,
       _tokenStorage = tokenStorage;

  final ApiClient _apiClient;
  final TokenStorage _tokenStorage;

  @override
  Future<AuthSession> login({
    required String email,
    required String password,
  }) async {
    final requestDto = LoginRequestDto(email: email, password: password);
    final session = await _apiClient.post<AuthSession>(
      ApiEndpoints.authLogin,
      requiresAuth: false,
      body: requestDto.toJson(),
      parser: _parseAuthSession,
    );
    await _persistSession(session);
    return session;
  }

  @override
  Future<AuthSession> register({
    required String fullName,
    required String email,
    required String password,
  }) async {
    final requestDto = RegisterRequestDto(
      fullName: fullName,
      email: email,
      password: password,
    );

    final session = await _apiClient.post<AuthSession>(
      ApiEndpoints.authRegister,
      requiresAuth: false,
      body: requestDto.toJson(),
      parser: _parseAuthSession,
    );
    await _persistSession(session);
    return session;
  }

  @override
  Future<void> requestPasswordReset({required String email}) async {
    final requestDto = PasswordResetRequestDto(email: email);
    await _apiClient.post<void>(
      ApiEndpoints.authPasswordResetRequest,
      requiresAuth: false,
      body: requestDto.toJson(),
      parser: (_) {},
    );
  }

  @override
  Future<void> confirmPasswordReset({
    required String token,
    required String newPassword,
  }) async {
    final requestDto = PasswordResetConfirmRequestDto(
      token: token,
      newPassword: newPassword,
    );
    await _apiClient.post<void>(
      ApiEndpoints.authPasswordResetConfirm,
      requiresAuth: false,
      body: requestDto.toJson(),
      parser: (_) {},
    );
  }

  @override
  Future<UserProfile> getProfile() {
    return _apiClient.get<UserProfile>(
      ApiEndpoints.accountProfile,
      parser: _parseUserProfile,
      requiresAuth: true,
      retryable: false,
    );
  }

  @override
  Future<UserProfile> updateProfile({
    required String fullName,
    required String phone,
  }) {
    final requestDto = UpdateProfileRequestDto(
      fullName: fullName,
      phone: phone,
    );
    return _apiClient.put<UserProfile>(
      ApiEndpoints.accountProfile,
      body: requestDto.toJson(),
      parser: _parseUserProfile,
      requiresAuth: true,
      retryable: false,
    );
  }

  @override
  Future<bool> hasSession() {
    return _tokenStorage.hasSession();
  }

  @override
  Future<void> logout() {
    return _tokenStorage.clear();
  }

  AuthSession _parseAuthSession(Object? value) {
    if (value is! Map<String, dynamic>) {
      throw const UnknownAppException(
        message: 'Invalid authentication payload from server.',
      );
    }
    final session = AuthSession.fromJson(value);
    if (session.accessToken.isEmpty || session.refreshToken.isEmpty) {
      throw const UnknownAppException(
        message: 'Authentication response is missing token fields.',
      );
    }
    return session;
  }

  UserProfile _parseUserProfile(Object? value) {
    if (value is! Map<String, dynamic>) {
      throw const UnknownAppException(
        message: 'Invalid user profile payload from server.',
      );
    }
    return UserProfile.fromJson(value);
  }

  Future<void> _persistSession(AuthSession session) {
    return _tokenStorage.saveTokens(
      accessToken: session.accessToken,
      refreshToken: session.refreshToken,
    );
  }
}

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  final tokenStorage = ref.watch(tokenStorageProvider);
  return AuthRepositoryImpl(apiClient: apiClient, tokenStorage: tokenStorage);
});

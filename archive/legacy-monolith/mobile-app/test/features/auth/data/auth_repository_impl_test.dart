import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:noura/core/network/api_client.dart';
import 'package:noura/core/network/api_endpoints.dart';
import 'package:noura/core/storage/token_storage.dart';
import 'package:noura/features/auth/data/repositories/auth_repository_impl.dart';

class FakeApiClient extends ApiClient {
  FakeApiClient() : super(Dio());

  String? lastPath;
  Object? lastBody;
  bool? lastRequiresAuth;
  Object? nextResponse;

  @override
  Future<T> post<T>(
    String path, {
    Object? body,
    Map<String, dynamic>? queryParameters,
    JsonParser<T>? parser,
    bool requiresAuth = true,
    bool retryable = false,
    CancelToken? cancelToken,
  }) async {
    lastPath = path;
    lastBody = body;
    lastRequiresAuth = requiresAuth;
    if (parser != null) {
      final dynamic parsed = parser(nextResponse);
      return parsed as T;
    }
    return nextResponse as T;
  }

  @override
  Future<T> get<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    JsonParser<T>? parser,
    bool requiresAuth = true,
    bool retryable = true,
    CancelToken? cancelToken,
  }) async {
    lastPath = path;
    lastRequiresAuth = requiresAuth;
    if (parser != null) {
      final dynamic parsed = parser(nextResponse);
      return parsed as T;
    }
    return nextResponse as T;
  }

  @override
  Future<T> put<T>(
    String path, {
    Object? body,
    Map<String, dynamic>? queryParameters,
    JsonParser<T>? parser,
    bool requiresAuth = true,
    bool retryable = false,
    CancelToken? cancelToken,
  }) async {
    lastPath = path;
    lastBody = body;
    lastRequiresAuth = requiresAuth;
    if (parser != null) {
      final dynamic parsed = parser(nextResponse);
      return parsed as T;
    }
    return nextResponse as T;
  }
}

class FakeTokenStorage implements TokenStorage {
  String? accessToken;
  String? refreshToken;

  @override
  Future<void> clear() async {
    accessToken = null;
    refreshToken = null;
  }

  @override
  Future<bool> hasSession() async {
    return (accessToken ?? '').isNotEmpty;
  }

  @override
  Future<String?> readAccessToken() async => accessToken;

  @override
  Future<String?> readRefreshToken() async => refreshToken;

  @override
  Future<void> saveTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }
}

void main() {
  group('AuthRepositoryImpl', () {
    late FakeApiClient apiClient;
    late FakeTokenStorage tokenStorage;
    late AuthRepositoryImpl repository;

    setUp(() {
      apiClient = FakeApiClient();
      tokenStorage = FakeTokenStorage();
      repository = AuthRepositoryImpl(
        apiClient: apiClient,
        tokenStorage: tokenStorage,
      );
    });

    test('login persists access and refresh tokens', () async {
      apiClient.nextResponse = <String, dynamic>{
        'userId': '11111111-1111-1111-1111-111111111111',
        'email': 'user@noura.com',
        'fullName': 'Noura User',
        'roles': <String>['CUSTOMER'],
        'accessToken': 'access-token-value',
        'refreshToken': 'refresh-token-value',
      };

      final session = await repository.login(
        email: 'user@noura.com',
        password: 'password123',
      );

      expect(apiClient.lastPath, ApiEndpoints.authLogin);
      expect(apiClient.lastRequiresAuth, isFalse);
      expect(session.accessToken, 'access-token-value');
      expect(tokenStorage.accessToken, 'access-token-value');
      expect(tokenStorage.refreshToken, 'refresh-token-value');
    });

    test('requestPasswordReset uses public endpoint payload', () async {
      await repository.requestPasswordReset(email: 'reset@noura.com');

      expect(apiClient.lastPath, ApiEndpoints.authPasswordResetRequest);
      expect(apiClient.lastRequiresAuth, isFalse);
      expect(apiClient.lastBody, <String, dynamic>{'email': 'reset@noura.com'});
    });

    test('getProfile parses response into user profile', () async {
      tokenStorage.accessToken = 'token';
      apiClient.nextResponse = <String, dynamic>{
        'id': '22222222-2222-2222-2222-222222222222',
        'fullName': 'Profile User',
        'email': 'profile@noura.com',
        'phone': '010101010',
        'roles': <String>['CUSTOMER'],
        'enabled': true,
        'preferredStoreId': '33333333-3333-3333-3333-333333333333',
      };

      final profile = await repository.getProfile();

      expect(apiClient.lastPath, ApiEndpoints.accountProfile);
      expect(apiClient.lastRequiresAuth, isTrue);
      expect(profile.fullName, 'Profile User');
      expect(profile.email, 'profile@noura.com');
      expect(profile.roles.contains('CUSTOMER'), isTrue);
    });
  });
}

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:noura/core/network/api_client.dart';
import 'package:noura/core/network/api_endpoints.dart';
import 'package:noura/features/account/data/repositories/account_repository_impl.dart';

class FakeApiClient extends ApiClient {
  FakeApiClient() : super(Dio());

  String? lastPath;
  Object? nextResponse;

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
    if (parser != null) {
      final dynamic parsed = parser(nextResponse);
      return parsed as T;
    }
    return nextResponse as T;
  }

  @override
  Future<T> patch<T>(
    String path, {
    Object? body,
    Map<String, dynamic>? queryParameters,
    JsonParser<T>? parser,
    bool requiresAuth = true,
    bool retryable = false,
    CancelToken? cancelToken,
  }) async {
    lastPath = path;
    if (parser != null) {
      final dynamic parsed = parser(nextResponse);
      return parsed as T;
    }
    return nextResponse as T;
  }
}

void main() {
  group('AccountRepositoryImpl', () {
    late FakeApiClient apiClient;
    late AccountRepositoryImpl repository;

    setUp(() {
      apiClient = FakeApiClient();
      repository = AccountRepositoryImpl(apiClient: apiClient);
    });

    test('getNotifications maps notification list', () async {
      apiClient.nextResponse = <Map<String, dynamic>>[
        <String, dynamic>{
          'id': 'n-1',
          'targetUserId': 'u-1',
          'category': 'ORDER',
          'title': 'Order shipped',
          'body': 'Your order is on the way.',
          'read': false,
          'createdAt': '2026-03-11T10:00:00Z',
        },
      ];

      final notifications = await repository.getNotifications();

      expect(apiClient.lastPath, ApiEndpoints.notificationsMe);
      expect(notifications, hasLength(1));
      expect(notifications.first.title, 'Order shipped');
      expect(notifications.first.read, isFalse);
    });

    test('markAllNotificationsAsRead maps integer response', () async {
      apiClient.nextResponse = 4;

      final affected = await repository.markAllNotificationsAsRead();

      expect(apiClient.lastPath, ApiEndpoints.notificationsReadAll);
      expect(affected, 4);
    });
  });
}

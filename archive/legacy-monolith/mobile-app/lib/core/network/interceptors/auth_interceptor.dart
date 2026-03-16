import 'dart:async';

import 'package:dio/dio.dart';

import '../models/api_envelope.dart';
import '../../storage/token_storage.dart';

class AuthInterceptor extends QueuedInterceptor {
  AuthInterceptor({
    required Dio dio,
    required TokenStorage tokenStorage,
    required String refreshPath,
    required void Function() onSessionExpired,
  }) : _dio = dio,
       _tokenStorage = tokenStorage,
       _refreshPath = refreshPath,
       _onSessionExpired = onSessionExpired,
       _refreshDio = Dio(dio.options);

  static const _retryFlag = '_auth_retry_attempted';

  final Dio _dio;
  final Dio _refreshDio;
  final TokenStorage _tokenStorage;
  final String _refreshPath;
  final void Function() _onSessionExpired;

  Completer<String?>? _refreshCompleter;
  bool _sessionExpiredNotified = false;

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    if (!_requiresAuth(options)) {
      handler.next(options);
      return;
    }

    final token = await _tokenStorage.readAccessToken();
    if (token != null && token.isNotEmpty) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final request = err.requestOptions;
    final statusCode = err.response?.statusCode;

    if (statusCode != 401 ||
        !_requiresAuth(request) ||
        request.extra[_retryFlag] == true ||
        request.path == _refreshPath) {
      handler.next(err);
      return;
    }

    final nextAccessToken = await _refreshAccessToken();
    if (nextAccessToken == null || nextAccessToken.isEmpty) {
      handler.next(err);
      return;
    }

    final retryRequest = request.copyWith(
      headers: <String, dynamic>{
        ...request.headers,
        'Authorization': 'Bearer $nextAccessToken',
      },
      extra: <String, dynamic>{...request.extra, _retryFlag: true},
    );

    try {
      final response = await _dio.fetch<dynamic>(retryRequest);
      handler.resolve(response);
    } on DioException catch (retryError) {
      handler.next(retryError);
    }
  }

  bool _requiresAuth(RequestOptions options) {
    return options.extra['requiresAuth'] != false;
  }

  Future<String?> _refreshAccessToken() async {
    final inFlightRefresh = _refreshCompleter;
    if (inFlightRefresh != null) {
      return inFlightRefresh.future;
    }

    final completer = Completer<String?>();
    _refreshCompleter = completer;

    try {
      final refreshToken = await _tokenStorage.readRefreshToken();
      if (refreshToken == null || refreshToken.isEmpty) {
        await _clearSessionAndNotify();
        completer.complete(null);
        return completer.future;
      }

      final response = await _refreshDio.post<dynamic>(
        _refreshPath,
        data: {'refreshToken': refreshToken},
        options: Options(extra: const <String, dynamic>{'requiresAuth': false}),
      );

      final body = _normalizeMap(response.data);
      final envelope = ApiEnvelope.fromJson(body);
      final payload = _normalizeMap(envelope.data);

      final accessToken = payload['accessToken'] as String?;
      final nextRefreshToken =
          payload['refreshToken'] as String? ?? refreshToken;

      if (envelope.success &&
          accessToken != null &&
          accessToken.isNotEmpty &&
          nextRefreshToken.isNotEmpty) {
        await _tokenStorage.saveTokens(
          accessToken: accessToken,
          refreshToken: nextRefreshToken,
        );
        _sessionExpiredNotified = false;
        completer.complete(accessToken);
      } else {
        await _clearSessionAndNotify();
        completer.complete(null);
      }
    } on Object {
      await _clearSessionAndNotify();
      completer.complete(null);
    } finally {
      _refreshCompleter = null;
    }

    return completer.future;
  }

  Future<void> _clearSessionAndNotify() async {
    await _tokenStorage.clear();
    if (!_sessionExpiredNotified) {
      _sessionExpiredNotified = true;
      _onSessionExpired();
    }
  }

  Map<String, dynamic> _normalizeMap(Object? value) {
    if (value is Map<String, dynamic>) {
      return value;
    }
    if (value is Map) {
      return value.map<String, dynamic>(
        (Object? key, Object? mapValue) => MapEntry(key.toString(), mapValue),
      );
    }
    return <String, dynamic>{};
  }
}

import 'package:dio/dio.dart';

import '../error/app_exception.dart';
import '../error/error_mapper.dart';
import 'models/api_envelope.dart';

typedef JsonParser<T> = T Function(Object? json);

class ApiClient {
  ApiClient(this._dio);

  final Dio _dio;

  Future<T> get<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    JsonParser<T>? parser,
    bool requiresAuth = true,
    bool retryable = true,
    CancelToken? cancelToken,
  }) {
    return _request<T>(
      path: path,
      method: 'GET',
      queryParameters: queryParameters,
      parser: parser,
      requiresAuth: requiresAuth,
      retryable: retryable,
      cancelToken: cancelToken,
    );
  }

  Future<T> post<T>(
    String path, {
    Object? body,
    Map<String, dynamic>? queryParameters,
    JsonParser<T>? parser,
    bool requiresAuth = true,
    bool retryable = false,
    CancelToken? cancelToken,
  }) {
    return _request<T>(
      path: path,
      method: 'POST',
      body: body,
      queryParameters: queryParameters,
      parser: parser,
      requiresAuth: requiresAuth,
      retryable: retryable,
      cancelToken: cancelToken,
    );
  }

  Future<T> put<T>(
    String path, {
    Object? body,
    Map<String, dynamic>? queryParameters,
    JsonParser<T>? parser,
    bool requiresAuth = true,
    bool retryable = false,
    CancelToken? cancelToken,
  }) {
    return _request<T>(
      path: path,
      method: 'PUT',
      body: body,
      queryParameters: queryParameters,
      parser: parser,
      requiresAuth: requiresAuth,
      retryable: retryable,
      cancelToken: cancelToken,
    );
  }

  Future<T> patch<T>(
    String path, {
    Object? body,
    Map<String, dynamic>? queryParameters,
    JsonParser<T>? parser,
    bool requiresAuth = true,
    bool retryable = false,
    CancelToken? cancelToken,
  }) {
    return _request<T>(
      path: path,
      method: 'PATCH',
      body: body,
      queryParameters: queryParameters,
      parser: parser,
      requiresAuth: requiresAuth,
      retryable: retryable,
      cancelToken: cancelToken,
    );
  }

  Future<T> delete<T>(
    String path, {
    Object? body,
    Map<String, dynamic>? queryParameters,
    JsonParser<T>? parser,
    bool requiresAuth = true,
    bool retryable = false,
    CancelToken? cancelToken,
  }) {
    return _request<T>(
      path: path,
      method: 'DELETE',
      body: body,
      queryParameters: queryParameters,
      parser: parser,
      requiresAuth: requiresAuth,
      retryable: retryable,
      cancelToken: cancelToken,
    );
  }

  Future<T> _request<T>({
    required String path,
    required String method,
    Object? body,
    Map<String, dynamic>? queryParameters,
    JsonParser<T>? parser,
    required bool requiresAuth,
    required bool retryable,
    CancelToken? cancelToken,
  }) async {
    try {
      final response = await _dio.request<Object?>(
        path,
        data: body,
        queryParameters: queryParameters,
        cancelToken: cancelToken,
        options: Options(
          method: method,
          extra: <String, dynamic>{
            'requiresAuth': requiresAuth,
            'retryable': retryable,
          },
        ),
      );

      return _parseEnvelope<T>(response, parser);
    } on Object catch (error) {
      throw ErrorMapper.map(error);
    }
  }

  T _parseEnvelope<T>(Response<Object?> response, JsonParser<T>? parser) {
    final responseMap = _normalizeMap(response.data);
    if (responseMap.isEmpty) {
      throw const UnknownAppException(
        message: 'The server returned an empty response payload.',
      );
    }

    final envelope = ApiEnvelope.fromJson(responseMap);
    if (!envelope.success) {
      throw ServerAppException(
        message:
            envelope.error?.detail ??
            envelope.message ??
            'Request failed. Please try again.',
        code: envelope.error?.code,
        statusCode: response.statusCode,
      );
    }

    if (parser == null) {
      return envelope.data as T;
    }

    return parser(envelope.data);
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

import 'dart:io';
import 'dart:math' as math;

import 'package:dio/dio.dart';

class RetryInterceptor extends Interceptor {
  RetryInterceptor({
    required Dio dio,
    this.maxRetries = 2,
    this.baseDelayMs = 250,
  }) : _dio = dio;

  static const _attemptKey = '_retry_attempt';
  static const _retryableMethods = {'GET', 'HEAD', 'OPTIONS'};

  final Dio _dio;
  final int maxRetries;
  final int baseDelayMs;

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final requestOptions = err.requestOptions;
    final attempt = (requestOptions.extra[_attemptKey] as int?) ?? 0;

    if (!_shouldRetry(err, attempt)) {
      handler.next(err);
      return;
    }

    final waitMs = baseDelayMs * math.pow(2, attempt).toInt();
    await Future<void>.delayed(Duration(milliseconds: waitMs));

    requestOptions.extra[_attemptKey] = attempt + 1;

    try {
      final response = await _dio.fetch<dynamic>(requestOptions);
      handler.resolve(response);
    } on DioException catch (retryError) {
      handler.next(retryError);
    }
  }

  bool _shouldRetry(DioException error, int attempt) {
    if (attempt >= maxRetries) {
      return false;
    }

    final method = error.requestOptions.method.toUpperCase();
    final explicitRetryable = error.requestOptions.extra['retryable'] == true;
    final retryableMethod =
        _retryableMethods.contains(method) || explicitRetryable;

    if (!retryableMethod) {
      return false;
    }

    final status = error.response?.statusCode;
    final transientStatus =
        status == 429 || status == 502 || status == 503 || status == 504;

    final transientNetwork =
        error.type == DioExceptionType.connectionError ||
        error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.sendTimeout ||
        error.type == DioExceptionType.receiveTimeout ||
        (error.type == DioExceptionType.unknown &&
            error.error is SocketException);

    return transientStatus || transientNetwork;
  }
}

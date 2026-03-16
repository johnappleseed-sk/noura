import 'dart:io';

import 'package:dio/dio.dart';

import '../network/models/api_envelope.dart';
import 'app_exception.dart';

abstract final class ErrorMapper {
  static AppException map(Object error) {
    if (error is AppException) {
      return error;
    }

    if (error is DioException) {
      return _mapDioException(error);
    }

    if (error is SocketException) {
      return const NetworkUnavailableException();
    }

    return UnknownAppException(message: error.toString());
  }

  static AppException _mapDioException(DioException error) {
    if (_isTimeout(error)) {
      return const TimeoutAppException();
    }

    if (error.error is SocketException) {
      return const NetworkUnavailableException();
    }

    final statusCode = error.response?.statusCode;
    final apiError = _extractApiError(error.response?.data);

    switch (statusCode) {
      case 400:
      case 422:
        return ValidationAppException(
          message: apiError.message,
          code: apiError.code,
          statusCode: statusCode,
        );
      case 401:
        return UnauthorizedAppException(
          message: apiError.message,
          code: apiError.code,
          statusCode: statusCode,
        );
      case 403:
        return ForbiddenAppException(
          message: apiError.message,
          code: apiError.code,
          statusCode: statusCode,
        );
      default:
        if (statusCode != null && statusCode >= 500) {
          return ServerAppException(
            message: apiError.message,
            code: apiError.code,
            statusCode: statusCode,
          );
        }
    }

    return UnknownAppException(
      message: apiError.message,
      code: apiError.code,
      statusCode: statusCode,
    );
  }

  static bool _isTimeout(DioException error) {
    return error.type == DioExceptionType.connectionTimeout ||
        error.type == DioExceptionType.sendTimeout ||
        error.type == DioExceptionType.receiveTimeout;
  }

  static _ApiErrorPayload _extractApiError(Object? payload) {
    if (payload is! Map<String, dynamic>) {
      return const _ApiErrorPayload(
        code: null,
        message: 'Request failed. Please try again.',
      );
    }

    final envelope = ApiEnvelope.fromJson(payload);
    return _ApiErrorPayload(
      code: envelope.error?.code,
      message:
          envelope.error?.detail ??
          envelope.message ??
          'Request failed. Please try again.',
    );
  }
}

class _ApiErrorPayload {
  const _ApiErrorPayload({required this.code, required this.message});

  final String? code;
  final String message;
}

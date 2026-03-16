sealed class AppException implements Exception {
  const AppException({required this.message, this.code, this.statusCode});

  final String message;
  final String? code;
  final int? statusCode;

  @override
  String toString() {
    final segments = <String>[
      'message=$message',
      if (code != null) 'code=$code',
      if (statusCode != null) 'statusCode=$statusCode',
    ];
    return 'AppException(${segments.join(', ')})';
  }
}

final class NetworkUnavailableException extends AppException {
  const NetworkUnavailableException({
    super.message = 'No internet connection. Please try again.',
  });
}

final class TimeoutAppException extends AppException {
  const TimeoutAppException({
    super.message = 'Request timeout. Please try again.',
  });
}

final class UnauthorizedAppException extends AppException {
  const UnauthorizedAppException({
    super.message = 'Your session has expired. Please login again.',
    super.code,
    super.statusCode = 401,
  });
}

final class ForbiddenAppException extends AppException {
  const ForbiddenAppException({
    super.message = 'You do not have permission to perform this action.',
    super.code,
    super.statusCode = 403,
  });
}

final class ValidationAppException extends AppException {
  const ValidationAppException({
    required super.message,
    super.code,
    super.statusCode = 400,
  });
}

final class ServerAppException extends AppException {
  const ServerAppException({
    required super.message,
    super.code,
    super.statusCode,
  });
}

final class UnknownAppException extends AppException {
  const UnknownAppException({
    super.message = 'Something went wrong. Please try again.',
    super.code,
    super.statusCode,
  });
}

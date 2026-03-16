import 'app_exception.dart';

abstract final class ErrorPresenter {
  static String message(Object error) {
    if (error is AppException) {
      return sanitize(error.message);
    }
    return sanitize(error.toString());
  }

  static String sanitize(String rawMessage) {
    final message = rawMessage.trim();
    if (message.isEmpty) {
      return 'Something went wrong. Please try again.';
    }

    if (message.startsWith('AppException(')) {
      final extracted = _extractAppExceptionMessage(message);
      if (extracted != null && extracted.isNotEmpty) {
        return extracted;
      }
    }

    if (message.startsWith('Exception:')) {
      final stripped = message.replaceFirst('Exception:', '').trim();
      if (stripped.isNotEmpty) {
        return stripped;
      }
    }

    if (message.startsWith('Error:')) {
      final stripped = message.replaceFirst('Error:', '').trim();
      if (stripped.isNotEmpty) {
        return stripped;
      }
    }

    return message;
  }

  static String? _extractAppExceptionMessage(String input) {
    const marker = 'message=';
    final start = input.indexOf(marker);
    if (start < 0) {
      return null;
    }

    final from = start + marker.length;
    final codeIdx = input.indexOf(', code=', from);
    final statusIdx = input.indexOf(', statusCode=', from);
    final endParenIdx = input.lastIndexOf(')');

    final candidates = <int>[
      codeIdx,
      statusIdx,
      endParenIdx,
    ].where((index) => index >= 0).toList(growable: false);
    if (candidates.isEmpty) {
      return null;
    }

    final end = candidates.reduce((a, b) => a < b ? a : b);
    if (end <= from) {
      return null;
    }
    return input.substring(from, end).trim();
  }
}

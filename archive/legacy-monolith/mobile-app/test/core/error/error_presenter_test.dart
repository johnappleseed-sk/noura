import 'package:flutter_test/flutter_test.dart';
import 'package:noura/core/error/app_exception.dart';
import 'package:noura/core/error/error_presenter.dart';

void main() {
  group('ErrorPresenter.message', () {
    test('returns app exception message', () {
      const error = ValidationAppException(message: 'Invalid request.');
      expect(ErrorPresenter.message(error), 'Invalid request.');
    });

    test('normalizes generic exception prefix', () {
      expect(
        ErrorPresenter.message(Exception('Something failed.')),
        'Something failed.',
      );
    });

    test('extracts message from AppException toString payload', () {
      const raw = 'AppException(message=Session expired, statusCode=401)';
      expect(ErrorPresenter.sanitize(raw), 'Session expired');
    });
  });
}

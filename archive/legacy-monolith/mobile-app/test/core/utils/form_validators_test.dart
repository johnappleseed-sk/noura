import 'package:flutter_test/flutter_test.dart';
import 'package:noura/core/utils/form_validators.dart';

void main() {
  group('FormValidators.email', () {
    test('returns error for invalid email', () {
      expect(
        FormValidators.email('invalid-email'),
        'Please enter a valid email address.',
      );
    });

    test('returns null for valid email', () {
      expect(FormValidators.email('user@noura.com'), isNull);
    });
  });

  group('FormValidators.password', () {
    test('returns error for short password', () {
      expect(
        FormValidators.password('1234567'),
        'Password must be at least 8 characters.',
      );
    });

    test('returns null for valid password', () {
      expect(FormValidators.password('12345678'), isNull);
    });
  });

  group('FormValidators.phone', () {
    test('returns null for empty optional phone', () {
      expect(FormValidators.phone(''), isNull);
    });

    test('returns error for invalid phone format', () {
      expect(FormValidators.phone('abc'), 'Please enter a valid phone number.');
    });

    test('returns null for valid phone format', () {
      expect(FormValidators.phone('+1 (555) 123-4567'), isNull);
    });
  });

  group('FormValidators.zipCode', () {
    test('returns error for empty zip code', () {
      expect(FormValidators.zipCode(''), 'Zip code is required.');
    });

    test('returns error for short zip code', () {
      expect(
        FormValidators.zipCode('12'),
        'Zip code must be 3 to 12 characters.',
      );
    });

    test('returns null for valid zip code', () {
      expect(FormValidators.zipCode('12000'), isNull);
    });
  });
}

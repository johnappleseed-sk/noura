abstract final class FormValidators {
  static String? requiredField(String? value, {String fieldName = 'Field'}) {
    if (value == null || value.trim().isEmpty) {
      return '$fieldName is required.';
    }
    return null;
  }

  static String? email(String? value) {
    final requiredValidation = requiredField(value, fieldName: 'Email');
    if (requiredValidation != null) {
      return requiredValidation;
    }

    final input = value!.trim();
    const emailRegex = r'^[^@\s]+@[^@\s]+\.[^@\s]+$';
    if (!RegExp(emailRegex).hasMatch(input)) {
      return 'Please enter a valid email address.';
    }
    return null;
  }

  static String? password(String? value, {int minLength = 8}) {
    final requiredValidation = requiredField(value, fieldName: 'Password');
    if (requiredValidation != null) {
      return requiredValidation;
    }

    final input = value!.trim();
    if (input.length < minLength) {
      return 'Password must be at least $minLength characters.';
    }
    return null;
  }

  static String? confirmPassword(String? value, String originalPassword) {
    final requiredValidation = requiredField(
      value,
      fieldName: 'Confirm password',
    );
    if (requiredValidation != null) {
      return requiredValidation;
    }

    if (value!.trim() != originalPassword.trim()) {
      return 'Passwords do not match.';
    }
    return null;
  }

  static String? phone(String? value, {bool required = false}) {
    final input = value?.trim() ?? '';
    if (!required && input.isEmpty) {
      return null;
    }
    if (required && input.isEmpty) {
      return 'Phone number is required.';
    }
    const phoneRegex = r'^[+0-9 ()-]{7,20}$';
    if (!RegExp(phoneRegex).hasMatch(input)) {
      return 'Please enter a valid phone number.';
    }
    return null;
  }

  static String? zipCode(String? value) {
    final requiredValidation = requiredField(value, fieldName: 'Zip code');
    if (requiredValidation != null) {
      return requiredValidation;
    }
    final input = value!.trim();
    if (input.length < 3 || input.length > 12) {
      return 'Zip code must be 3 to 12 characters.';
    }
    return null;
  }
}

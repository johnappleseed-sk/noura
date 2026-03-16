class PasswordResetRequestDto {
  const PasswordResetRequestDto({required this.email});

  final String email;

  Map<String, dynamic> toJson() {
    return <String, dynamic>{'email': email.trim()};
  }
}

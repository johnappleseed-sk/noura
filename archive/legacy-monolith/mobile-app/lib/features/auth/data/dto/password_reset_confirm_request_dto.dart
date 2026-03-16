class PasswordResetConfirmRequestDto {
  const PasswordResetConfirmRequestDto({
    required this.token,
    required this.newPassword,
  });

  final String token;
  final String newPassword;

  Map<String, dynamic> toJson() {
    return <String, dynamic>{'token': token.trim(), 'newPassword': newPassword};
  }
}

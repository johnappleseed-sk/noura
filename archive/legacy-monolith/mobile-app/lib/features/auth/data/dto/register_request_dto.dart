class RegisterRequestDto {
  const RegisterRequestDto({
    required this.fullName,
    required this.email,
    required this.password,
  });

  final String fullName;
  final String email;
  final String password;

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'fullName': fullName.trim(),
      'email': email.trim(),
      'password': password,
    };
  }
}

class AuthSession {
  const AuthSession({
    required this.userId,
    required this.email,
    required this.fullName,
    required this.roles,
    required this.accessToken,
    required this.refreshToken,
  });

  factory AuthSession.fromJson(Map<String, dynamic> json) {
    return AuthSession(
      userId: (json['userId'] ?? '').toString(),
      email: (json['email'] ?? '').toString(),
      fullName: (json['fullName'] ?? '').toString(),
      roles: (json['roles'] as List<dynamic>? ?? const [])
          .map((dynamic value) => value.toString())
          .toSet(),
      accessToken: (json['accessToken'] ?? '').toString(),
      refreshToken: (json['refreshToken'] ?? '').toString(),
    );
  }

  final String userId;
  final String email;
  final String fullName;
  final Set<String> roles;
  final String accessToken;
  final String refreshToken;
}

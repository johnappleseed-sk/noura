import 'auth_session.dart';

class UserProfile {
  const UserProfile({
    required this.id,
    required this.fullName,
    required this.email,
    required this.phone,
    required this.roles,
    required this.enabled,
    required this.preferredStoreId,
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      id: (json['id'] ?? '').toString(),
      fullName: (json['fullName'] ?? '').toString(),
      email: (json['email'] ?? '').toString(),
      phone: (json['phone'] as String?) ?? '',
      roles: (json['roles'] as List<dynamic>? ?? const [])
          .map((dynamic value) => value.toString())
          .toSet(),
      enabled: json['enabled'] == true,
      preferredStoreId: (json['preferredStoreId'] as String?) ?? '',
    );
  }

  factory UserProfile.fromSession(AuthSession session) {
    return UserProfile(
      id: session.userId,
      fullName: session.fullName,
      email: session.email,
      phone: '',
      roles: session.roles,
      enabled: true,
      preferredStoreId: '',
    );
  }

  final String id;
  final String fullName;
  final String email;
  final String phone;
  final Set<String> roles;
  final bool enabled;
  final String preferredStoreId;

  UserProfile copyWith({
    String? id,
    String? fullName,
    String? email,
    String? phone,
    Set<String>? roles,
    bool? enabled,
    String? preferredStoreId,
  }) {
    return UserProfile(
      id: id ?? this.id,
      fullName: fullName ?? this.fullName,
      email: email ?? this.email,
      phone: phone ?? this.phone,
      roles: roles ?? this.roles,
      enabled: enabled ?? this.enabled,
      preferredStoreId: preferredStoreId ?? this.preferredStoreId,
    );
  }
}

import '../domain/entities/user_profile.dart';

class AuthSessionState {
  const AuthSessionState._({required this.isAuthenticated, this.profile});

  const AuthSessionState.unauthenticated()
    : this._(isAuthenticated: false, profile: null);

  const AuthSessionState.authenticated({required UserProfile profile})
    : this._(isAuthenticated: true, profile: profile);

  final bool isAuthenticated;
  final UserProfile? profile;
}

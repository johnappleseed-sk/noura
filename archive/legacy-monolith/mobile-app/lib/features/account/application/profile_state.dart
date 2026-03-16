import '../../auth/domain/entities/user_profile.dart';

class ProfileState {
  const ProfileState({
    required this.profile,
    required this.isSaving,
    this.message,
    this.error,
  });

  final UserProfile profile;
  final bool isSaving;
  final String? message;
  final String? error;

  ProfileState copyWith({
    UserProfile? profile,
    bool? isSaving,
    Object? message = _unset,
    Object? error = _unset,
  }) {
    return ProfileState(
      profile: profile ?? this.profile,
      isSaving: isSaving ?? this.isSaving,
      message: identical(message, _unset) ? this.message : message as String?,
      error: identical(error, _unset) ? this.error : error as String?,
    );
  }

  static const Object _unset = Object();
}

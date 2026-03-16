import '../domain/entities/app_notification.dart';

class NotificationsState {
  const NotificationsState({
    required this.items,
    required this.unreadCount,
    required this.isMutating,
    this.message,
    this.error,
  });

  const NotificationsState.initial()
    : items = const <AppNotification>[],
      unreadCount = 0,
      isMutating = false,
      message = null,
      error = null;

  final List<AppNotification> items;
  final int unreadCount;
  final bool isMutating;
  final String? message;
  final String? error;

  NotificationsState copyWith({
    List<AppNotification>? items,
    int? unreadCount,
    bool? isMutating,
    Object? message = _unset,
    Object? error = _unset,
  }) {
    return NotificationsState(
      items: items ?? this.items,
      unreadCount: unreadCount ?? this.unreadCount,
      isMutating: isMutating ?? this.isMutating,
      message: identical(message, _unset) ? this.message : message as String?,
      error: identical(error, _unset) ? this.error : error as String?,
    );
  }

  static const Object _unset = Object();
}

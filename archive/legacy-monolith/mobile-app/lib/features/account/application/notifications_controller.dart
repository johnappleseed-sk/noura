import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/error/app_exception.dart';
import '../../../core/error/error_presenter.dart';
import '../data/repositories/account_repository_impl.dart';
import '../domain/entities/app_notification.dart';
import 'notifications_state.dart';

class NotificationsController extends AsyncNotifier<NotificationsState> {
  @override
  Future<NotificationsState> build() async {
    return _load();
  }

  Future<void> refresh() async {
    state = const AsyncLoading<NotificationsState>();
    state = await AsyncValue.guard<NotificationsState>(_load);
  }

  Future<bool> markAsRead(String notificationId) async {
    final current = state.valueOrNull;
    if (current == null) {
      return false;
    }

    state = AsyncData<NotificationsState>(
      current.copyWith(isMutating: true, error: null, message: null),
    );

    try {
      final updated = await ref
          .read(accountRepositoryProvider)
          .markNotificationAsRead(notificationId: notificationId);

      final nextItems = current.items
          .map((item) => item.id == updated.id ? updated : item)
          .toList(growable: false);
      final nextUnread = nextItems.where((item) => !item.read).length;

      state = AsyncData<NotificationsState>(
        NotificationsState(
          items: nextItems,
          unreadCount: nextUnread,
          isMutating: false,
        ),
      );
      return true;
    } on AppException catch (error) {
      state = AsyncData<NotificationsState>(
        current.copyWith(
          isMutating: false,
          error: error.message,
          message: null,
        ),
      );
      return false;
    } on Object catch (error) {
      state = AsyncData<NotificationsState>(
        current.copyWith(
          isMutating: false,
          error: ErrorPresenter.message(error),
          message: null,
        ),
      );
      return false;
    }
  }

  Future<bool> markAllAsRead() async {
    final current = state.valueOrNull;
    if (current == null) {
      return false;
    }

    state = AsyncData<NotificationsState>(
      current.copyWith(isMutating: true, error: null, message: null),
    );

    try {
      final affected = await ref
          .read(accountRepositoryProvider)
          .markAllNotificationsAsRead();
      final nextItems = current.items
          .map((item) => item.copyWith(read: true))
          .toList(growable: false);
      state = AsyncData<NotificationsState>(
        NotificationsState(
          items: nextItems,
          unreadCount: 0,
          isMutating: false,
          message: affected > 0
              ? '$affected notifications marked as read.'
              : 'No unread notifications.',
        ),
      );
      return true;
    } on AppException catch (error) {
      state = AsyncData<NotificationsState>(
        current.copyWith(
          isMutating: false,
          error: error.message,
          message: null,
        ),
      );
      return false;
    } on Object catch (error) {
      state = AsyncData<NotificationsState>(
        current.copyWith(
          isMutating: false,
          error: ErrorPresenter.message(error),
          message: null,
        ),
      );
      return false;
    }
  }

  void clearMessages() {
    final current = state.valueOrNull;
    if (current == null) {
      return;
    }
    state = AsyncData<NotificationsState>(
      current.copyWith(error: null, message: null),
    );
  }

  Future<NotificationsState> _load() async {
    final results = await Future.wait<Object>([
      ref.read(accountRepositoryProvider).getNotifications(),
      ref.read(accountRepositoryProvider).getUnreadNotificationsCount(),
    ]);

    final notifications = (results[0] as List<AppNotification>)
      ..sort((a, b) {
        final aDate = a.createdAt?.millisecondsSinceEpoch ?? 0;
        final bDate = b.createdAt?.millisecondsSinceEpoch ?? 0;
        return bDate.compareTo(aDate);
      });

    final unreadCount = (results[1] as int).clamp(0, notifications.length);
    return NotificationsState(
      items: notifications,
      unreadCount: unreadCount,
      isMutating: false,
    );
  }
}

final notificationsControllerProvider =
    AsyncNotifierProvider<NotificationsController, NotificationsState>(
      NotificationsController.new,
    );

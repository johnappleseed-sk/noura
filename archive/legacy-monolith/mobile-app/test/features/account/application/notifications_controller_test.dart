import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:noura/features/account/application/notifications_controller.dart';
import 'package:noura/features/account/data/repositories/account_repository_impl.dart';
import 'package:noura/features/account/domain/entities/app_notification.dart';
import 'package:noura/features/account/domain/entities/runtime_features.dart';
import 'package:noura/features/account/domain/entities/store_location.dart';
import 'package:noura/features/account/domain/repositories/account_repository.dart';
import 'package:noura/features/shopping/domain/entities/order.dart';

class FakeAccountRepository implements AccountRepository {
  FakeAccountRepository();

  List<AppNotification> _notifications = <AppNotification>[
    AppNotification(
      id: 'n-1',
      targetUserId: 'u-1',
      category: 'ORDER',
      title: 'Order shipped',
      body: 'Shipment update',
      read: false,
      createdAt: DateTime.parse('2026-03-11T10:00:00Z'),
    ),
    AppNotification(
      id: 'n-2',
      targetUserId: 'u-1',
      category: 'SYSTEM',
      title: 'Welcome',
      body: 'Hello',
      read: true,
      createdAt: DateTime.parse('2026-03-10T10:00:00Z'),
    ),
  ];

  @override
  Future<List<Order>> getOrderHistory() async {
    return const <Order>[];
  }

  @override
  Future<List<Order>> quickReorder({required String orderId}) async {
    return const <Order>[];
  }

  @override
  Future<List<AppNotification>> getNotifications() async {
    return _notifications;
  }

  @override
  Future<int> getUnreadNotificationsCount() async {
    return _notifications.where((item) => !item.read).length;
  }

  @override
  Future<AppNotification> markNotificationAsRead({
    required String notificationId,
  }) async {
    _notifications = _notifications
        .map(
          (item) =>
              item.id == notificationId ? item.copyWith(read: true) : item,
        )
        .toList(growable: false);
    return _notifications.firstWhere((item) => item.id == notificationId);
  }

  @override
  Future<int> markAllNotificationsAsRead() async {
    final unread = _notifications.where((item) => !item.read).length;
    _notifications = _notifications
        .map((item) => item.copyWith(read: true))
        .toList(growable: false);
    return unread;
  }

  @override
  Future<RuntimeFeatures> getRuntimeFeatures() async {
    return const RuntimeFeatures(
      contractVersion: '',
      features: <String, bool>{},
      messages: <String, String>{},
    );
  }

  @override
  Future<List<StoreLocation>> getStores({
    int page = 0,
    int size = 20,
    String sortBy = 'name',
    String direction = 'asc',
    String? service,
    bool? openNow,
  }) async {
    return const <StoreLocation>[];
  }

  @override
  Future<void> setPreferredStore({required String storeId}) async {}
}

void main() {
  test('NotificationsController marks all notifications as read', () async {
    final container = ProviderContainer(
      overrides: [
        accountRepositoryProvider.overrideWithValue(FakeAccountRepository()),
      ],
    );
    addTearDown(container.dispose);

    final initial = await container.read(
      notificationsControllerProvider.future,
    );
    expect(initial.unreadCount, 1);

    final ok = await container
        .read(notificationsControllerProvider.notifier)
        .markAllAsRead();
    expect(ok, isTrue);

    final state = container.read(notificationsControllerProvider).valueOrNull;
    expect(state?.unreadCount, 0);
    expect(state?.items.every((item) => item.read), isTrue);
  });
}

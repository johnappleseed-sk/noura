import '../../../shopping/domain/entities/order.dart';
import '../entities/app_notification.dart';
import '../entities/runtime_features.dart';
import '../entities/store_location.dart';

abstract class AccountRepository {
  Future<List<Order>> getOrderHistory();

  Future<List<Order>> quickReorder({required String orderId});

  Future<List<AppNotification>> getNotifications();

  Future<int> getUnreadNotificationsCount();

  Future<AppNotification> markNotificationAsRead({
    required String notificationId,
  });

  Future<int> markAllNotificationsAsRead();

  Future<RuntimeFeatures> getRuntimeFeatures();

  Future<List<StoreLocation>> getStores({
    int page = 0,
    int size = 20,
    String sortBy = 'name',
    String direction = 'asc',
    String? service,
    bool? openNow,
  });

  Future<void> setPreferredStore({required String storeId});
}

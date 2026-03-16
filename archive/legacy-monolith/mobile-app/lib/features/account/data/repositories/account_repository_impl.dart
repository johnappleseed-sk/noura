import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_client.dart';
import '../../../../core/network/api_endpoints.dart';
import '../../../../core/providers/core_providers.dart';
import '../../../shopping/domain/entities/order.dart';
import '../../domain/entities/app_notification.dart';
import '../../domain/entities/runtime_features.dart';
import '../../domain/entities/store_location.dart';
import '../../domain/repositories/account_repository.dart';

class AccountRepositoryImpl implements AccountRepository {
  AccountRepositoryImpl({required ApiClient apiClient})
    : _apiClient = apiClient;

  final ApiClient _apiClient;

  @override
  Future<List<Order>> getOrderHistory() {
    return _apiClient.get<List<Order>>(
      ApiEndpoints.accountOrders,
      parser: (Object? value) =>
          _parseList(value).map(Order.fromJson).toList(growable: false),
    );
  }

  @override
  Future<List<Order>> quickReorder({required String orderId}) {
    return _apiClient.post<List<Order>>(
      ApiEndpoints.accountOrderQuickReorder(orderId),
      parser: (Object? value) =>
          _parseList(value).map(Order.fromJson).toList(growable: false),
    );
  }

  @override
  Future<List<AppNotification>> getNotifications() {
    return _apiClient.get<List<AppNotification>>(
      ApiEndpoints.notificationsMe,
      parser: (Object? value) => _parseList(
        value,
      ).map(AppNotification.fromJson).toList(growable: false),
    );
  }

  @override
  Future<int> getUnreadNotificationsCount() {
    return _apiClient.get<int>(
      ApiEndpoints.notificationsUnreadCount,
      parser: _parseInt,
    );
  }

  @override
  Future<AppNotification> markNotificationAsRead({
    required String notificationId,
  }) {
    return _apiClient.patch<AppNotification>(
      ApiEndpoints.notificationRead(notificationId),
      parser: (Object? value) => AppNotification.fromJson(_parseMap(value)),
    );
  }

  @override
  Future<int> markAllNotificationsAsRead() {
    return _apiClient.patch<int>(
      ApiEndpoints.notificationsReadAll,
      parser: _parseInt,
    );
  }

  @override
  Future<RuntimeFeatures> getRuntimeFeatures() {
    return _apiClient.get<RuntimeFeatures>(
      ApiEndpoints.runtimeFeatures,
      parser: (Object? value) => RuntimeFeatures.fromJson(_parseMap(value)),
      requiresAuth: false,
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
  }) {
    final query = <String, dynamic>{
      'page': page,
      'size': size,
      'sortBy': sortBy,
      'direction': direction,
      if (service != null && service.isNotEmpty) 'service': service,
      if (openNow case final bool value) 'openNow': value,
    };

    return _apiClient.get<List<StoreLocation>>(
      ApiEndpoints.stores,
      queryParameters: query,
      parser: (Object? value) {
        final map = _parseMap(value);
        return _parseList(
          map['content'],
        ).map(StoreLocation.fromJson).toList(growable: false);
      },
    );
  }

  @override
  Future<void> setPreferredStore({required String storeId}) {
    return _apiClient.put<void>(
      ApiEndpoints.storesPreferred(storeId),
      parser: (_) {},
    );
  }

  List<Map<String, dynamic>> _parseList(Object? value) {
    if (value is List<dynamic>) {
      return value.whereType<Map<String, dynamic>>().toList(growable: false);
    }
    return const <Map<String, dynamic>>[];
  }

  Map<String, dynamic> _parseMap(Object? value) {
    if (value is Map<String, dynamic>) {
      return value;
    }
    if (value is Map) {
      return value.map<String, dynamic>(
        (Object? key, Object? mapValue) => MapEntry(key.toString(), mapValue),
      );
    }
    return const <String, dynamic>{};
  }

  int _parseInt(Object? value) {
    if (value is num) {
      return value.toInt();
    }
    if (value is String) {
      return int.tryParse(value) ?? 0;
    }
    return 0;
  }
}

final accountRepositoryProvider = Provider<AccountRepository>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return AccountRepositoryImpl(apiClient: apiClient);
});

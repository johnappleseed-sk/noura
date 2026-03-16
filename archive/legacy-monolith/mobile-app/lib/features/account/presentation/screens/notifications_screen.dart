import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../../core/widgets/app_empty_view.dart';
import '../../../../core/widgets/app_error_view.dart';
import '../../../../core/widgets/app_loading_view.dart';
import '../../application/notifications_controller.dart';
import '../../application/notifications_state.dart';

class NotificationsScreen extends ConsumerWidget {
  const NotificationsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final notificationsAsync = ref.watch(notificationsControllerProvider);
    final dateFormatter = DateFormat('yyyy-MM-dd HH:mm');

    return Scaffold(
      appBar: AppBar(
        title: const Text('Notifications'),
        actions: [
          notificationsAsync.when(
            loading: () => const SizedBox.shrink(),
            error: (_, _) => const SizedBox.shrink(),
            data: (state) => TextButton(
              onPressed: state.isMutating || state.unreadCount == 0
                  ? null
                  : () => ref
                        .read(notificationsControllerProvider.notifier)
                        .markAllAsRead(),
              child: const Text('Mark all'),
            ),
          ),
        ],
      ),
      body: notificationsAsync.when(
        loading: () =>
            const AppLoadingView(message: 'Loading notifications...'),
        error: (Object error, StackTrace stackTrace) => AppErrorView(
          message: error.toString(),
          onRetry: () =>
              ref.read(notificationsControllerProvider.notifier).refresh(),
        ),
        data: (NotificationsState state) {
          if (state.items.isEmpty) {
            return const AppEmptyView(
              title: 'No notifications.',
              subtitle: 'Order and account updates will appear here.',
            );
          }

          return RefreshIndicator(
            onRefresh: () =>
                ref.read(notificationsControllerProvider.notifier).refresh(),
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
              children: [
                _StatusCard(state: state),
                const SizedBox(height: 10),
                ...state.items.map((notification) {
                  return Card(
                    margin: const EdgeInsets.only(bottom: 10),
                    child: ListTile(
                      onTap: notification.read || state.isMutating
                          ? null
                          : () => ref
                                .read(notificationsControllerProvider.notifier)
                                .markAsRead(notification.id),
                      leading: CircleAvatar(
                        backgroundColor: notification.read
                            ? Theme.of(
                                context,
                              ).colorScheme.surfaceContainerHighest
                            : Theme.of(context).colorScheme.primaryContainer,
                        child: Icon(
                          _iconForCategory(notification.category),
                          size: 18,
                        ),
                      ),
                      title: Text(notification.title),
                      subtitle: Text(
                        [
                          notification.body,
                          if (notification.createdAt != null)
                            dateFormatter.format(
                              notification.createdAt!.toLocal(),
                            ),
                        ].where((entry) => entry.trim().isNotEmpty).join('\n'),
                      ),
                      isThreeLine: true,
                      trailing: notification.read
                          ? const Icon(Icons.done, size: 18)
                          : const Icon(Icons.fiber_new, size: 18),
                    ),
                  );
                }),
              ],
            ),
          );
        },
      ),
    );
  }

  IconData _iconForCategory(String category) {
    switch (category) {
      case 'ORDER':
        return Icons.local_shipping_outlined;
      case 'SECURITY':
        return Icons.security_outlined;
      case 'STORE':
        return Icons.storefront_outlined;
      case 'AI':
        return Icons.auto_awesome_outlined;
      case 'SYSTEM':
      default:
        return Icons.notifications_none;
    }
  }
}

class _StatusCard extends ConsumerWidget {
  const _StatusCard({required this.state});

  final NotificationsState state;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Unread: ${state.unreadCount}',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            if (state.error != null) ...[
              const SizedBox(height: 8),
              Text(
                state.error!,
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
            ],
            if (state.message != null) ...[
              const SizedBox(height: 8),
              Text(state.message!),
            ],
            if (state.message != null || state.error != null)
              Align(
                alignment: Alignment.centerRight,
                child: TextButton(
                  onPressed: () => ref
                      .read(notificationsControllerProvider.notifier)
                      .clearMessages(),
                  child: const Text('Dismiss'),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

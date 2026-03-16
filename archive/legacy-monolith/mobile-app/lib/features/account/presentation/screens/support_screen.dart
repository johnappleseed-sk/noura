import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/app_empty_view.dart';
import '../../../../core/widgets/app_error_view.dart';
import '../../../../core/widgets/app_loading_view.dart';
import '../../application/support_provider.dart';

class SupportScreen extends ConsumerWidget {
  const SupportScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final supportAsync = ref.watch(supportViewDataProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Support')),
      body: supportAsync.when(
        loading: () => const AppLoadingView(message: 'Loading support info...'),
        error: (Object error, StackTrace stackTrace) => AppErrorView(
          message: error.toString(),
          onRetry: () => ref.invalidate(supportViewDataProvider),
        ),
        data: (data) {
          return ListView(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
            children: [
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Need help?',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 8),
                      Text(data.contactMessage),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 10),
              ...data.topics.map(
                (topic) => Card(
                  margin: const EdgeInsets.only(bottom: 10),
                  child: ListTile(
                    title: Text(topic.title),
                    subtitle: Text(topic.summary),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () => context.goNamed(
                      AppRoutes.legalDocumentName,
                      pathParameters: <String, String>{'topicId': topic.id},
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 6),
              if (data.stores.isEmpty)
                const AppEmptyView(
                  title: 'No active stores found.',
                  subtitle: 'Store support contacts are not available yet.',
                )
              else
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Active Stores',
                          style: Theme.of(context).textTheme.titleMedium,
                        ),
                        const SizedBox(height: 8),
                        ...data.stores.map(
                          (store) => ListTile(
                            contentPadding: EdgeInsets.zero,
                            title: Text(store.name),
                            subtitle: Text(
                              [
                                store.compactAddress,
                                if (store.services.isNotEmpty)
                                  'Services: ${store.services.join(', ')}',
                              ].join('\n'),
                            ),
                            isThreeLine: true,
                            trailing: store.openNow
                                ? const Chip(label: Text('Open'))
                                : const Chip(label: Text('Closed')),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
            ],
          );
        },
      ),
    );
  }
}

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/widgets/app_empty_view.dart';
import '../../../../core/widgets/app_error_view.dart';
import '../../../../core/widgets/app_loading_view.dart';
import '../../application/support_provider.dart';

class LegalDocumentScreen extends ConsumerWidget {
  const LegalDocumentScreen({required this.topicId, super.key});

  final String topicId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (topicId.trim().isEmpty) {
      return const Scaffold(
        body: AppEmptyView(
          title: 'Document unavailable.',
          subtitle: 'The requested legal document is missing.',
        ),
      );
    }

    final documentAsync = ref.watch(supportTopicByIdProvider(topicId));

    return Scaffold(
      appBar: AppBar(title: const Text('Legal')),
      body: documentAsync.when(
        loading: () => const AppLoadingView(message: 'Loading document...'),
        error: (Object error, StackTrace stackTrace) => AppErrorView(
          message: error.toString(),
          onRetry: () => ref.invalidate(supportTopicByIdProvider(topicId)),
        ),
        data: (topic) {
          if (topic == null) {
            return const AppEmptyView(
              title: 'Document unavailable.',
              subtitle: 'The requested legal document could not be found.',
            );
          }

          return SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  topic.title,
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                const SizedBox(height: 8),
                Text(
                  topic.summary,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 18),
                SelectableText(
                  topic.content,
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

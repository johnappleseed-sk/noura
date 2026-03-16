import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/repositories/account_repository_impl.dart';
import '../domain/entities/runtime_features.dart';
import '../domain/entities/store_location.dart';
import '../domain/entities/support_topic.dart';

class SupportViewData {
  const SupportViewData({
    required this.topics,
    required this.stores,
    required this.runtimeFeatures,
    required this.contactMessage,
  });

  final List<SupportTopic> topics;
  final List<StoreLocation> stores;
  final RuntimeFeatures runtimeFeatures;
  final String contactMessage;
}

final supportViewDataProvider = FutureProvider<SupportViewData>((ref) async {
  final repository = ref.read(accountRepositoryProvider);

  final results = await Future.wait<Object>([
    repository.getRuntimeFeatures(),
    repository.getStores(size: 20),
  ]);

  final runtimeFeatures = results[0] as RuntimeFeatures;
  final stores = results[1] as List<StoreLocation>;

  final topics = _buildSupportTopics(runtimeFeatures);
  final contactMessage = _resolveMessage(
    runtimeFeatures,
    keys: const <String>['support.contact', 'support.message', 'contact'],
    fallback:
        'Reach support through your nearest store or account notifications.',
  );

  return SupportViewData(
    topics: topics,
    stores: stores.where((store) => store.active).toList(growable: false),
    runtimeFeatures: runtimeFeatures,
    contactMessage: contactMessage,
  );
});

final supportTopicByIdProvider = FutureProvider.family<SupportTopic?, String>((
  ref,
  topicId,
) async {
  final supportData = await ref.watch(supportViewDataProvider.future);
  final normalizedId = topicId.trim();
  if (normalizedId.isEmpty) {
    return null;
  }

  return supportData.topics.cast<SupportTopic?>().firstWhere(
    (topic) => topic?.id == normalizedId,
    orElse: () => null,
  );
});

List<SupportTopic> _buildSupportTopics(RuntimeFeatures runtimeFeatures) {
  // Backend does not expose CMS/legal page endpoints yet. Runtime messages
  // are used as the remote source, with deterministic defaults as fallback.
  return <SupportTopic>[
    SupportTopic(
      id: 'terms',
      title: 'Terms of Service',
      summary: 'Usage rules and customer obligations.',
      content: _resolveMessage(
        runtimeFeatures,
        keys: const <String>['terms', 'legal.terms', 'cms.terms'],
        fallback:
            'By using Noura, you agree to platform terms and responsible usage.',
      ),
    ),
    SupportTopic(
      id: 'privacy',
      title: 'Privacy Policy',
      summary: 'How account and order data are handled.',
      content: _resolveMessage(
        runtimeFeatures,
        keys: const <String>['privacy', 'legal.privacy', 'cms.privacy'],
        fallback:
            'We process profile, order, and notification data for commerce operations.',
      ),
    ),
    SupportTopic(
      id: 'about',
      title: 'About Noura',
      summary: 'Platform mission and service scope.',
      content: _resolveMessage(
        runtimeFeatures,
        keys: const <String>['about', 'cms.about'],
        fallback:
            'Noura is a commerce platform focused on reliable ordering and fulfillment.',
      ),
    ),
  ];
}

String _resolveMessage(
  RuntimeFeatures runtimeFeatures, {
  required List<String> keys,
  required String fallback,
}) {
  for (final key in keys) {
    final value = runtimeFeatures.messages[key]?.trim();
    if (value != null && value.isNotEmpty) {
      return value;
    }
  }
  return fallback;
}

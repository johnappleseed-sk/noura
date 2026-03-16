import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/repositories/commerce_repository_impl.dart';
import '../domain/entities/search_suggestion.dart';
import '../domain/entities/trend_tag.dart';

final predictiveSearchProvider =
    FutureProvider.family<List<SearchSuggestion>, String>((ref, query) async {
      final trimmed = query.trim();
      if (trimmed.isEmpty) {
        return const <SearchSuggestion>[];
      }
      final repository = ref.read(commerceRepositoryProvider);
      return repository.predictiveSearch(query: trimmed);
    });

final trendTagsProvider = FutureProvider<List<TrendTag>>((ref) async {
  final repository = ref.read(commerceRepositoryProvider);
  return repository.getTrendTags();
});

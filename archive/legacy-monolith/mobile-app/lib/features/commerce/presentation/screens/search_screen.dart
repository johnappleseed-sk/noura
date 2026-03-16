import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/constants/app_constants.dart';
import '../../../../core/widgets/app_empty_view.dart';
import '../../../../core/widgets/app_error_view.dart';
import '../../../../core/widgets/app_loading_view.dart';
import '../../application/search_providers.dart';

class SearchScreen extends ConsumerStatefulWidget {
  const SearchScreen({super.key});

  @override
  ConsumerState<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends ConsumerState<SearchScreen> {
  final TextEditingController _searchController = TextEditingController();
  Timer? _debounce;
  String _debouncedQuery = '';

  @override
  void dispose() {
    _debounce?.cancel();
    _searchController.dispose();
    super.dispose();
  }

  void _onSearchChanged(String value) {
    _debounce?.cancel();
    _debounce = Timer(
      const Duration(milliseconds: AppConstants.searchDebounceMs),
      () {
        if (!mounted) {
          return;
        }
        setState(() {
          _debouncedQuery = value.trim();
        });
      },
    );
  }

  void _navigateToProducts(String query) {
    context.go('${AppRoutes.productsPath}?q=${Uri.encodeComponent(query)}');
  }

  @override
  Widget build(BuildContext context) {
    final suggestionsState = ref.watch(
      predictiveSearchProvider(_debouncedQuery),
    );
    final trendTagsState = ref.watch(trendTagsProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Search')),
      body: Padding(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
        child: Column(
          children: [
            TextField(
              controller: _searchController,
              onChanged: _onSearchChanged,
              textInputAction: TextInputAction.search,
              onSubmitted: (value) {
                final query = value.trim();
                if (query.isNotEmpty) {
                  _navigateToProducts(query);
                }
              },
              decoration: const InputDecoration(
                hintText: 'Search products, brands, or categories...',
                prefixIcon: Icon(Icons.search),
              ),
            ),
            const SizedBox(height: 12),
            Expanded(
              child: _debouncedQuery.isEmpty
                  ? trendTagsState.when(
                      loading: () => const AppLoadingView(
                        message: 'Loading trend tags...',
                      ),
                      error: (Object error, StackTrace stackTrace) =>
                          AppErrorView(
                            message: error.toString(),
                            onRetry: () => ref.invalidate(trendTagsProvider),
                          ),
                      data: (tags) {
                        if (tags.isEmpty) {
                          return const AppEmptyView(
                            title: 'No trend tags available.',
                          );
                        }
                        return ListView(
                          children: [
                            Text(
                              'Trending searches',
                              style: Theme.of(context).textTheme.titleLarge,
                            ),
                            const SizedBox(height: 10),
                            Wrap(
                              spacing: 8,
                              runSpacing: 8,
                              children: tags
                                  .map(
                                    (tag) => ActionChip(
                                      label: Text(
                                        '${tag.value} (${tag.score})',
                                      ),
                                      onPressed: () {
                                        _searchController.text = tag.value;
                                        _onSearchChanged(tag.value);
                                        _navigateToProducts(tag.value);
                                      },
                                    ),
                                  )
                                  .toList(growable: false),
                            ),
                          ],
                        );
                      },
                    )
                  : suggestionsState.when(
                      loading: () =>
                          const AppLoadingView(message: 'Searching...'),
                      error: (Object error, StackTrace stackTrace) =>
                          AppErrorView(
                            message: error.toString(),
                            onRetry: () => ref.invalidate(
                              predictiveSearchProvider(_debouncedQuery),
                            ),
                          ),
                      data: (suggestions) {
                        if (suggestions.isEmpty) {
                          return AppEmptyView(
                            title: 'No suggestions found.',
                            subtitle: 'Search directly for "$_debouncedQuery".',
                            actionLabel: 'Search products',
                            onActionPressed: () =>
                                _navigateToProducts(_debouncedQuery),
                          );
                        }
                        return ListView.separated(
                          itemCount: suggestions.length,
                          separatorBuilder: (BuildContext context, int index) =>
                              const Divider(height: 1),
                          itemBuilder: (context, index) {
                            final suggestion = suggestions[index];
                            return ListTile(
                              leading: const Icon(Icons.trending_up),
                              title: Text(suggestion.value),
                              subtitle: suggestion.scope.isEmpty
                                  ? null
                                  : Text(suggestion.scope),
                              onTap: () =>
                                  _navigateToProducts(suggestion.value),
                            );
                          },
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }
}

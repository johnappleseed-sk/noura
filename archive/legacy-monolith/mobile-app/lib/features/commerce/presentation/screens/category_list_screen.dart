import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/widgets/app_empty_view.dart';
import '../../../../core/widgets/app_error_view.dart';
import '../../../../core/widgets/app_loading_view.dart';
import '../../application/categories_provider.dart';

class CategoryListScreen extends ConsumerWidget {
  const CategoryListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final categoriesState = ref.watch(categoriesProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Categories')),
      body: categoriesState.when(
        loading: () => const AppLoadingView(message: 'Loading categories...'),
        error: (Object error, StackTrace stackTrace) => AppErrorView(
          message: error.toString(),
          onRetry: () => ref.invalidate(categoriesProvider),
        ),
        data: (categories) {
          if (categories.isEmpty) {
            return const AppEmptyView(title: 'No categories found.');
          }

          return RefreshIndicator(
            onRefresh: () async {
              ref.invalidate(categoriesProvider);
              await ref.read(categoriesProvider.future);
            },
            child: ListView.separated(
              itemCount: categories.length,
              separatorBuilder: (BuildContext context, int index) =>
                  const Divider(height: 1),
              itemBuilder: (context, index) {
                final category = categories[index];
                return ListTile(
                  title: Text(category.name),
                  subtitle: category.description.isNotEmpty
                      ? Text(category.description)
                      : null,
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.go(
                    '${AppRoutes.productsPath}'
                    '?categoryId=${Uri.encodeComponent(category.id)}'
                    '&categoryName=${Uri.encodeComponent(category.name)}',
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }
}

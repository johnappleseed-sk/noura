import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/repositories/commerce_repository_impl.dart';
import '../domain/entities/category_node.dart';

final categoriesProvider = FutureProvider<List<CategoryNode>>((ref) async {
  final repository = ref.read(commerceRepositoryProvider);
  final roots = await repository.getCategories();
  return roots.expand((CategoryNode node) => node.flatten()).toList();
});

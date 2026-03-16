import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/repositories/commerce_repository_impl.dart';
import '../domain/entities/product_detail.dart';

final productDetailProvider = FutureProvider.family<ProductDetail, String>((
  ref,
  productId,
) async {
  final repository = ref.read(commerceRepositoryProvider);
  return repository.getProductDetail(productId: productId);
});

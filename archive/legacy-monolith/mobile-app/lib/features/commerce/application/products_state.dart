import '../domain/entities/product_card.dart';
import 'product_list_filter.dart';

class ProductsState {
  const ProductsState({
    required this.filter,
    required this.items,
    required this.page,
    required this.hasNext,
    required this.isLoadingMore,
    required this.paginationError,
  });

  final ProductListFilter filter;
  final List<ProductCard> items;
  final int page;
  final bool hasNext;
  final bool isLoadingMore;
  final String? paginationError;

  ProductsState copyWith({
    ProductListFilter? filter,
    List<ProductCard>? items,
    int? page,
    bool? hasNext,
    bool? isLoadingMore,
    String? paginationError,
  }) {
    return ProductsState(
      filter: filter ?? this.filter,
      items: items ?? this.items,
      page: page ?? this.page,
      hasNext: hasNext ?? this.hasNext,
      isLoadingMore: isLoadingMore ?? this.isLoadingMore,
      paginationError: paginationError,
    );
  }
}

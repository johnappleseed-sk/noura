class ProductListFilter {
  const ProductListFilter({
    this.query,
    this.categoryId,
    this.sort = 'featured',
    this.pageSize = 12,
  });

  final String? query;
  final String? categoryId;
  final String sort;
  final int pageSize;

  ProductListFilter copyWith({
    String? query,
    String? categoryId,
    String? sort,
    int? pageSize,
  }) {
    return ProductListFilter(
      query: query ?? this.query,
      categoryId: categoryId ?? this.categoryId,
      sort: sort ?? this.sort,
      pageSize: pageSize ?? this.pageSize,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }
    return other is ProductListFilter &&
        other.query == query &&
        other.categoryId == categoryId &&
        other.sort == sort &&
        other.pageSize == pageSize;
  }

  @override
  int get hashCode => Object.hash(query, categoryId, sort, pageSize);
}

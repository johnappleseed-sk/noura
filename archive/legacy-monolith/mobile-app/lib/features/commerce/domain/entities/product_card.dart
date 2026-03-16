class ProductCard {
  const ProductCard({
    required this.id,
    required this.name,
    required this.categoryId,
    required this.categoryName,
    required this.price,
    required this.compareAtPrice,
    required this.imageUrl,
    required this.stockQty,
    required this.lowStock,
    required this.allowNegativeStock,
    required this.isNew,
    required this.isTrending,
    required this.isBestseller,
    required this.merchandisingScore,
  });

  factory ProductCard.fromJson(Map<String, dynamic> json) {
    return ProductCard(
      id: (json['id'] ?? '').toString(),
      name: (json['name'] as String?) ?? '',
      categoryId: (json['categoryId'] ?? '').toString(),
      categoryName: (json['categoryName'] as String?) ?? '',
      price: _toDouble(json['price']),
      compareAtPrice: _toNullableDouble(json['compareAtPrice']),
      imageUrl: (json['imageUrl'] as String?) ?? '',
      stockQty: _toInt(json['stockQty']),
      lowStock: json['lowStock'] == true,
      allowNegativeStock: json['allowNegativeStock'] == true,
      isNew: json['isNew'] == true,
      isTrending: json['isTrending'] == true,
      isBestseller: json['isBestseller'] == true,
      merchandisingScore: _toDouble(json['merchandisingScore']),
    );
  }

  final String id;
  final String name;
  final String categoryId;
  final String categoryName;
  final double price;
  final double? compareAtPrice;
  final String imageUrl;
  final int stockQty;
  final bool lowStock;
  final bool allowNegativeStock;
  final bool isNew;
  final bool isTrending;
  final bool isBestseller;
  final double merchandisingScore;

  static double _toDouble(Object? value) {
    if (value is num) {
      return value.toDouble();
    }
    if (value is String) {
      return double.tryParse(value) ?? 0;
    }
    return 0;
  }

  static double? _toNullableDouble(Object? value) {
    if (value == null) {
      return null;
    }
    if (value is num) {
      return value.toDouble();
    }
    if (value is String) {
      return double.tryParse(value);
    }
    return null;
  }

  static int _toInt(Object? value) {
    if (value is num) {
      return value.toInt();
    }
    if (value is String) {
      return int.tryParse(value) ?? 0;
    }
    return 0;
  }
}

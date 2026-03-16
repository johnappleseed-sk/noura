class RecommendationProduct {
  const RecommendationProduct({
    required this.id,
    required this.name,
    required this.categoryId,
    required this.categoryName,
    required this.price,
    required this.imageUrl,
    required this.shortDescription,
    required this.score,
    required this.reason,
  });

  factory RecommendationProduct.fromJson(Map<String, dynamic> json) {
    return RecommendationProduct(
      id: (json['id'] ?? '').toString(),
      name: (json['name'] as String?) ?? '',
      categoryId: (json['categoryId'] ?? '').toString(),
      categoryName: (json['categoryName'] as String?) ?? '',
      price: _toDouble(json['price']),
      imageUrl: (json['imageUrl'] as String?) ?? '',
      shortDescription: (json['shortDescription'] as String?) ?? '',
      score: _toDouble(json['score']),
      reason: (json['reason'] as String?) ?? '',
    );
  }

  final String id;
  final String name;
  final String categoryId;
  final String categoryName;
  final double price;
  final String imageUrl;
  final String shortDescription;
  final double score;
  final String reason;

  static double _toDouble(Object? value) {
    if (value is num) {
      return value.toDouble();
    }
    if (value is String) {
      return double.tryParse(value) ?? 0;
    }
    return 0;
  }
}

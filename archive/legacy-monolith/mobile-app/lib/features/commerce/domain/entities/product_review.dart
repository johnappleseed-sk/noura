class ProductReview {
  const ProductReview({
    required this.id,
    required this.userId,
    required this.userName,
    required this.rating,
    required this.comment,
  });

  factory ProductReview.fromJson(Map<String, dynamic> json) {
    return ProductReview(
      id: (json['id'] ?? '').toString(),
      userId: (json['userId'] ?? '').toString(),
      userName: (json['userName'] as String?)?.trim().isNotEmpty == true
          ? (json['userName'] as String).trim()
          : 'Anonymous',
      rating: _toRating(json['rating']),
      comment: (json['comment'] as String?)?.trim() ?? '',
    );
  }

  final String id;
  final String userId;
  final String userName;
  final int rating;
  final String comment;

  static int _toRating(Object? value) {
    if (value is num) {
      final normalized = value.toInt();
      if (normalized < 1) {
        return 1;
      }
      if (normalized > 5) {
        return 5;
      }
      return normalized;
    }

    if (value is String) {
      final parsed = int.tryParse(value);
      if (parsed == null) {
        return 5;
      }
      if (parsed < 1) {
        return 1;
      }
      if (parsed > 5) {
        return 5;
      }
      return parsed;
    }
    return 5;
  }
}

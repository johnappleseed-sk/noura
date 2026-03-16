class TrendTag {
  const TrendTag({required this.value, required this.score});

  factory TrendTag.fromJson(Map<String, dynamic> json) {
    return TrendTag(
      value: (json['value'] as String?) ?? '',
      score: json['score'] is num ? (json['score'] as num).toInt() : 0,
    );
  }

  final String value;
  final int score;
}

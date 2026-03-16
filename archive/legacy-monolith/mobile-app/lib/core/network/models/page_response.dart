class PageResponse<T> {
  const PageResponse({
    required this.content,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
    required this.first,
    required this.last,
  });

  factory PageResponse.fromJson(
    Map<String, dynamic> json, {
    required T Function(Map<String, dynamic> json) fromJsonT,
  }) {
    final rawContent = (json['content'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>();

    return PageResponse<T>(
      content: rawContent.map(fromJsonT).toList(growable: false),
      page: (json['page'] as num?)?.toInt() ?? 0,
      size: (json['size'] as num?)?.toInt() ?? 0,
      totalElements: (json['totalElements'] as num?)?.toInt() ?? 0,
      totalPages: (json['totalPages'] as num?)?.toInt() ?? 0,
      first: json['first'] == true,
      last: json['last'] == true,
    );
  }

  final List<T> content;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;
  final bool first;
  final bool last;

  bool get hasNext => !last;
}

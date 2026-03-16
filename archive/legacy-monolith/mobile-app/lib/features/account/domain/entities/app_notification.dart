class AppNotification {
  const AppNotification({
    required this.id,
    required this.targetUserId,
    required this.category,
    required this.title,
    required this.body,
    required this.read,
    required this.createdAt,
  });

  factory AppNotification.fromJson(Map<String, dynamic> json) {
    return AppNotification(
      id: (json['id'] ?? '').toString(),
      targetUserId: (json['targetUserId'] ?? '').toString(),
      category: (json['category'] as String?) ?? '',
      title: (json['title'] as String?) ?? '',
      body: (json['body'] as String?) ?? '',
      read: json['read'] == true,
      createdAt: _toDateTime(json['createdAt']),
    );
  }

  final String id;
  final String targetUserId;
  final String category;
  final String title;
  final String body;
  final bool read;
  final DateTime? createdAt;

  AppNotification copyWith({bool? read}) {
    return AppNotification(
      id: id,
      targetUserId: targetUserId,
      category: category,
      title: title,
      body: body,
      read: read ?? this.read,
      createdAt: createdAt,
    );
  }

  static DateTime? _toDateTime(Object? value) {
    if (value is String && value.trim().isNotEmpty) {
      return DateTime.tryParse(value);
    }
    return null;
  }
}

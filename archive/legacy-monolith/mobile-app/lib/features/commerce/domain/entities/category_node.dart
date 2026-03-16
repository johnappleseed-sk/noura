class CategoryNode {
  const CategoryNode({
    required this.id,
    required this.name,
    required this.description,
    required this.children,
  });

  factory CategoryNode.fromJson(Map<String, dynamic> json) {
    final rawChildren = (json['children'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(CategoryNode.fromJson)
        .toList(growable: false);

    return CategoryNode(
      id: (json['id'] ?? '').toString(),
      name: (json['name'] as String?) ?? '',
      description: (json['description'] as String?) ?? '',
      children: rawChildren,
    );
  }

  final String id;
  final String name;
  final String description;
  final List<CategoryNode> children;

  List<CategoryNode> flatten() {
    final bucket = <CategoryNode>[this];
    for (final child in children) {
      bucket.addAll(child.flatten());
    }
    return bucket;
  }
}

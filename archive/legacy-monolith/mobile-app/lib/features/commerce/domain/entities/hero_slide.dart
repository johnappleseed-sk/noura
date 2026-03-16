class HeroSlide {
  const HeroSlide({
    required this.id,
    required this.title,
    required this.description,
    required this.imageDesktop,
    required this.imageMobile,
    required this.buttonText,
    required this.linkType,
    required this.linkValue,
    required this.openInNewTab,
  });

  factory HeroSlide.fromJson(Map<String, dynamic> json) {
    return HeroSlide(
      id: (json['id'] ?? '').toString(),
      title: (json['title'] as String?) ?? '',
      description: (json['description'] as String?) ?? '',
      imageDesktop: (json['imageDesktop'] as String?) ?? '',
      imageMobile: (json['imageMobile'] as String?) ?? '',
      buttonText: (json['buttonText'] as String?) ?? '',
      linkType: (json['linkType'] as String?) ?? '',
      linkValue: (json['linkValue'] as String?) ?? '',
      openInNewTab: json['openInNewTab'] == true,
    );
  }

  final String id;
  final String title;
  final String description;
  final String imageDesktop;
  final String imageMobile;
  final String buttonText;
  final String linkType;
  final String linkValue;
  final bool openInNewTab;

  String get bestImageUrl =>
      imageDesktop.isNotEmpty ? imageDesktop : imageMobile;
}

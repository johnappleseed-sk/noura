class RuntimeFeatures {
  const RuntimeFeatures({
    required this.contractVersion,
    required this.features,
    required this.messages,
  });

  factory RuntimeFeatures.fromJson(Map<String, dynamic> json) {
    final rawFeatures = json['features'];
    final rawMessages = json['messages'];

    return RuntimeFeatures(
      contractVersion: (json['contractVersion'] as String?) ?? '',
      features: rawFeatures is Map
          ? rawFeatures.map<String, bool>((key, value) {
              return MapEntry(key.toString(), value == true);
            })
          : const <String, bool>{},
      messages: rawMessages is Map
          ? rawMessages.map<String, String>((key, value) {
              return MapEntry(key.toString(), value?.toString() ?? '');
            })
          : const <String, String>{},
    );
  }

  final String contractVersion;
  final Map<String, bool> features;
  final Map<String, String> messages;
}

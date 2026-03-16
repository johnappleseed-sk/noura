class PaymentMethod {
  const PaymentMethod({
    required this.id,
    required this.methodType,
    required this.provider,
    required this.tokenizedReference,
    required this.defaultMethod,
  });

  factory PaymentMethod.fromJson(Map<String, dynamic> json) {
    return PaymentMethod(
      id: (json['id'] ?? '').toString(),
      methodType: (json['methodType'] as String?) ?? '',
      provider: (json['provider'] as String?) ?? '',
      tokenizedReference: (json['tokenizedReference'] as String?) ?? '',
      defaultMethod: json['defaultMethod'] == true,
    );
  }

  final String id;
  final String methodType;
  final String provider;
  final String tokenizedReference;
  final bool defaultMethod;

  String get displayName {
    final base = methodType.trim().isNotEmpty ? methodType : 'PAYMENT';
    final providerName = provider.trim();
    if (providerName.isEmpty) {
      return base;
    }
    return '$base • $providerName';
  }

  String get maskedReference {
    if (tokenizedReference.length <= 4) {
      return tokenizedReference;
    }
    return '•••• ${tokenizedReference.substring(tokenizedReference.length - 4)}';
  }
}

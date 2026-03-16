class PaymentMethodRequestDto {
  const PaymentMethodRequestDto({
    required this.methodType,
    required this.provider,
    required this.tokenizedReference,
    this.defaultMethod = false,
  });

  final String methodType;
  final String provider;
  final String tokenizedReference;
  final bool defaultMethod;

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'methodType': methodType,
      'provider': provider,
      'tokenizedReference': tokenizedReference,
      'defaultMethod': defaultMethod,
    };
  }
}

class CheckoutPaymentRequestDto {
  const CheckoutPaymentRequestDto({
    this.paymentReference,
    this.couponCode,
    this.b2bInvoice,
    this.idempotencyKey,
  });

  final String? paymentReference;
  final String? couponCode;
  final bool? b2bInvoice;
  final String? idempotencyKey;

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      if (paymentReference != null && paymentReference!.isNotEmpty)
        'paymentReference': paymentReference,
      if (couponCode != null && couponCode!.isNotEmpty)
        'couponCode': couponCode,
      if (b2bInvoice != null) 'b2bInvoice': b2bInvoice,
      if (idempotencyKey != null && idempotencyKey!.isNotEmpty)
        'idempotencyKey': idempotencyKey,
    };
  }
}

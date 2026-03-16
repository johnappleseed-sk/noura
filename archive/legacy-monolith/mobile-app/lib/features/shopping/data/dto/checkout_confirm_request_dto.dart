class CheckoutConfirmRequestDto {
  const CheckoutConfirmRequestDto({
    this.fulfillmentMethod,
    this.storeId,
    this.addressId,
    this.shippingAddressSnapshot,
    this.paymentReference,
    this.couponCode,
    this.b2bInvoice,
    this.idempotencyKey,
  });

  final String? fulfillmentMethod;
  final String? storeId;
  final String? addressId;
  final String? shippingAddressSnapshot;
  final String? paymentReference;
  final String? couponCode;
  final bool? b2bInvoice;
  final String? idempotencyKey;

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      if (fulfillmentMethod != null && fulfillmentMethod!.isNotEmpty)
        'fulfillmentMethod': fulfillmentMethod,
      if (storeId != null && storeId!.isNotEmpty) 'storeId': storeId,
      if (addressId != null && addressId!.isNotEmpty) 'addressId': addressId,
      if (shippingAddressSnapshot != null &&
          shippingAddressSnapshot!.isNotEmpty)
        'shippingAddressSnapshot': shippingAddressSnapshot,
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

class CheckoutShippingRequestDto {
  const CheckoutShippingRequestDto({
    required this.fulfillmentMethod,
    this.storeId,
    this.addressId,
    this.shippingAddressSnapshot,
  });

  final String fulfillmentMethod;
  final String? storeId;
  final String? addressId;
  final String? shippingAddressSnapshot;

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'fulfillmentMethod': fulfillmentMethod,
      if (storeId != null && storeId!.isNotEmpty) 'storeId': storeId,
      if (addressId != null && addressId!.isNotEmpty) 'addressId': addressId,
      if (shippingAddressSnapshot != null &&
          shippingAddressSnapshot!.isNotEmpty)
        'shippingAddressSnapshot': shippingAddressSnapshot,
    };
  }
}

class AddCartItemRequestDto {
  const AddCartItemRequestDto({
    required this.productId,
    this.variantId,
    this.quantity = 1,
    this.storeId,
    this.analyticsListName,
    this.analyticsSlot,
    this.analyticsPagePath,
  });

  final String productId;
  final String? variantId;
  final int quantity;
  final String? storeId;
  final String? analyticsListName;
  final int? analyticsSlot;
  final String? analyticsPagePath;

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'productId': productId,
      if (variantId != null && variantId!.isNotEmpty) 'variantId': variantId,
      'quantity': quantity,
      if (storeId != null && storeId!.isNotEmpty) 'storeId': storeId,
      if (analyticsListName != null && analyticsListName!.isNotEmpty)
        'analyticsListName': analyticsListName,
      if (analyticsSlot != null) 'analyticsSlot': analyticsSlot,
      if (analyticsPagePath != null && analyticsPagePath!.isNotEmpty)
        'analyticsPagePath': analyticsPagePath,
    };
  }
}
